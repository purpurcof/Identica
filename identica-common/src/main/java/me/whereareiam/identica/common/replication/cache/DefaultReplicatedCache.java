package me.whereareiam.identica.common.replication.cache;

import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.logging.Logger;
import me.whereareiam.identica.replication.cache.ReplicatedCache;
import me.whereareiam.identica.replication.ReplicationAdapter;
import me.whereareiam.identica.model.replication.ReplicationEnvelope;
import me.whereareiam.identica.model.replication.ReplicationPage;
import me.whereareiam.identica.model.replication.ReplicationType;
import me.whereareiam.identica.replication.codec.SnapshotCodec;
import me.whereareiam.identica.replication.codec.SnapshotCodecFactory;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@RequiredArgsConstructor
public final class DefaultReplicatedCache<T, S> implements ReplicatedCache<T> {
	private final String name;
	private final InMemoryLocalCache<T> localCache;
	private final ReplicationAdapter adapter;
	private final ReplicationType<T, S> type;
	private final SnapshotCodecFactory codecFactory;

	@Override
	public @NotNull CompletableFuture<Optional<T>> get(String key) {
		return localCache.get(key)
				.thenCompose(local -> {
					if (local.isPresent()) {
						return CompletableFuture.completedFuture(local);
					}
					return getFresh(key);
				});
	}

	@Override
	public @NotNull CompletableFuture<Optional<T>> getFresh(String key) {
		if (key == null) return CompletableFuture.completedFuture(Optional.empty());

		if (adapter == null || !adapter.isAvailable())
			return localCache.get(key);

		return adapter.get(name, key)
				.thenCompose(payload -> payload
						.map(data -> handleRemoteHit(key, data))
						.orElseGet(() -> localCache.invalidate(key)
								.thenApply(ignored -> {
									Logger.debug("Replicated cache miss %s:%s", name, key);
									return Optional.empty();
								})));
	}

	private CompletableFuture<Optional<T>> handleRemoteHit(String key, byte[] data) {
		ReplicationEnvelope envelope;
		try {
			envelope = ReplicationEnvelope.decode(data);
		} catch (Exception e) {
			return invalidate(key).thenApply(ignored -> Optional.empty());
		}

		if (envelope == null) return CompletableFuture.completedFuture(Optional.empty());
		if (envelope.getVersion() != type.version())
			return invalidate(key).thenApply(ignored -> Optional.empty());

		long now = System.currentTimeMillis();
		if (envelope.getExpiresAt() > 0 && envelope.getExpiresAt() <= now)
			return invalidate(key).thenApply(ignored -> Optional.empty());

		SnapshotCodec<S> codec = resolveCodec();
		S snapshot;
		try {
			snapshot = codec.decode(envelope.getPayload());
		} catch (Exception e) {
			return invalidate(key).thenApply(ignored -> Optional.empty());
		}

		if (snapshot == null) return invalidate(key).thenApply(ignored -> Optional.empty());

		long ttlMs = envelope.getExpiresAt() > 0 ? envelope.getExpiresAt() - now : 0;
		return type.mapper()
				.fromSnapshot(snapshot)
				.thenCompose(value -> {
					if (ttlMs > 0) {
						Logger.debug("Replicated cache hit %s:%s", name, key);
						return localCache.put(key, value, ttlMs)
								.thenApply(ignored -> Optional.ofNullable(value));
					}
					Logger.debug("Replicated cache hit %s:%s", name, key);
					return CompletableFuture.completedFuture(Optional.ofNullable(value));
				})
				.exceptionally(ignored -> Optional.empty());
	}

	@Override
	public @NotNull CompletableFuture<Void> put(String key, T value, long ttlMs) {
		CompletableFuture<Void> local = localCache.put(key, value, ttlMs);
		if (adapter == null || !adapter.isAvailable())
			return local;

		if (ttlMs <= 0) return local.thenCompose(ignored -> adapter.invalidate(name, key));

		long expiresAt = System.currentTimeMillis() + ttlMs;
		SnapshotCodec<S> codec = resolveCodec();
		S snapshot = type.mapper().toSnapshot(value);
		byte[] payload = codec.encode(snapshot);
		byte[] envelope = ReplicationEnvelope.encode(type.version(), expiresAt, payload);

		return local.thenCompose(ignored -> adapter.put(name, key, envelope, ttlMs));
	}

	@Override
	public @NotNull CompletableFuture<Void> invalidate(String key) {
		CompletableFuture<Void> local = localCache.invalidate(key);
		if (adapter == null || !adapter.isAvailable())
			return local;

		return local.thenCompose(ignored -> adapter.invalidate(name, key));
	}

	@Override
	public @NotNull CompletableFuture<Optional<T>> consume(String key) {
		if (key == null) return CompletableFuture.completedFuture(Optional.empty());

		if (adapter == null || !adapter.isAvailable()) return localCache.consume(key);

		return adapter.consume(name, key)
				.thenCompose(payload -> localCache.invalidate(key)
						.thenCompose(ignored -> payload
								.map(this::decodeConsumedValue)
								.orElseGet(() -> CompletableFuture.completedFuture(Optional.empty()))));
	}

	private CompletableFuture<Optional<T>> decodeConsumedValue(byte[] data) {
		ReplicationEnvelope envelope;
		try {
			envelope = ReplicationEnvelope.decode(data);
		} catch (Exception ignored) {
			return CompletableFuture.completedFuture(Optional.empty());
		}

		if (envelope == null || envelope.getVersion() != type.version())
			return CompletableFuture.completedFuture(Optional.empty());
		if (envelope.getExpiresAt() > 0 && envelope.getExpiresAt() <= System.currentTimeMillis())
			return CompletableFuture.completedFuture(Optional.empty());

		SnapshotCodec<S> codec = resolveCodec();
		try {
			S snapshot = codec.decode(envelope.getPayload());
			if (snapshot == null) return CompletableFuture.completedFuture(Optional.empty());
			return type.mapper().fromSnapshot(snapshot)
					.thenApply(Optional::ofNullable)
					.exceptionally(ignored -> Optional.empty());
		} catch (Exception ignored) {
			return CompletableFuture.completedFuture(Optional.empty());
		}
	}

	@Override
	public @NotNull CompletableFuture<ReplicationPage> listKeys(int page, int pageSize) {
		if (adapter == null || !adapter.isAvailable())
			return localCache.listKeys(page, pageSize);

		return adapter.listKeys(name, page, pageSize);
	}

	private SnapshotCodec<S> resolveCodec() {
		SnapshotCodec<S> override = type.codecOverride();
		if (override != null) return override;
		return codecFactory.codecFor(type.snapshotType());
	}
}
