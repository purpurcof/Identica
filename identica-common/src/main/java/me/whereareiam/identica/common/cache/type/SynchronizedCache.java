package me.whereareiam.identica.common.cache.type;

import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.cache.Cache;
import me.whereareiam.identica.cache.codec.CacheCodec;
import me.whereareiam.identica.logging.Logger;
import me.whereareiam.identica.service.SynchronizationService;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

@RequiredArgsConstructor
public final class SynchronizedCache<T> implements Cache<T> {
	private final String name;
	private final LocalCache<T> localCache;
	private final CacheCodec<T> codec;
	private final Supplier<SynchronizationService> serviceSupplier;

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

		SynchronizationService service = serviceSupplier.get();
		if (service == null || !service.isAvailable())
			return localCache.get(key);

		return service.get(name, key)
				.thenCompose(payload -> payload
						.map(data -> handleRemoteHit(key, data))
						.orElseGet(() -> localCache.invalidate(key).thenApply(ignored -> {
							Logger.debug("Synchronized cache miss %s:%s", name, key);
							return Optional.empty();
						})));
	}

	private CompletableFuture<Optional<T>> handleRemoteHit(String key, byte[] data) {
		Decoded decoded;
		try {
			decoded = decodeEnvelope(data);
		} catch (Exception e) {
			return invalidate(key).thenApply(ignored -> Optional.empty());
		}

		if (decoded == null) return CompletableFuture.completedFuture(Optional.empty());
		long now = System.currentTimeMillis();
		if (decoded.expiresAt > 0 && decoded.expiresAt <= now)
			return invalidate(key).thenApply(ignored -> Optional.empty());

		T value;
		try {
			value = codec.decode(decoded.payload);
		} catch (Exception e) {
			return invalidate(key).thenApply(ignored -> Optional.empty());
		}

		long ttlMs = decoded.expiresAt > 0 ? decoded.expiresAt - now : 0;
		if (ttlMs > 0) {
			Logger.debug("Synchronized cache hit %s:%s", name, key);
			return localCache
					.put(key, value, ttlMs)
					.thenApply(ignored -> Optional.ofNullable(value));
		}

		Logger.debug("Synchronized cache hit %s:%s", name, key);
		return CompletableFuture.completedFuture(Optional.ofNullable(value));
	}

	private CompletableFuture<Optional<T>> decodeConsumedValue(byte[] data) {
		Decoded decoded;
		try {
			decoded = decodeEnvelope(data);
		} catch (Exception ignored) {
			return CompletableFuture.completedFuture(Optional.empty());
		}

		if (decoded == null) return CompletableFuture.completedFuture(Optional.empty());
		if (decoded.expiresAt > 0 && decoded.expiresAt <= System.currentTimeMillis())
			return CompletableFuture.completedFuture(Optional.empty());

		try {
			return CompletableFuture.completedFuture(Optional.ofNullable(codec.decode(decoded.payload)));
		} catch (Exception ignored) {
			return CompletableFuture.completedFuture(Optional.empty());
		}
	}

	@Override
	public @NotNull CompletableFuture<Void> put(String key, T value, long ttlMs) {
		CompletableFuture<Void> local = localCache.put(key, value, ttlMs);
		SynchronizationService service = serviceSupplier.get();
		if (service == null || !service.isAvailable())
			return local;

		if (ttlMs <= 0) return local.thenCompose(ignored -> service.invalidate(name, key));

		long expiresAt = System.currentTimeMillis() + ttlMs;
		byte[] payload = codec.encode(value);
		byte[] envelope = encodeEnvelope(payload, expiresAt);

		return local.thenCompose(ignored -> service.put(name, key, envelope, ttlMs));
	}

	@Override
	public @NotNull CompletableFuture<Void> invalidate(String key) {
		CompletableFuture<Void> local = localCache.invalidate(key);
		SynchronizationService service = serviceSupplier.get();
		if (service == null || !service.isAvailable())
			return local;

		return local.thenCompose(ignored -> service.invalidate(name, key));
	}

	@Override
	public @NotNull CompletableFuture<Optional<T>> consume(String key) {
		if (key == null) return CompletableFuture.completedFuture(Optional.empty());

		SynchronizationService service = serviceSupplier.get();
		if (service == null || !service.isAvailable()) return localCache.consume(key);

		return service.consume(name, key)
				.thenCompose(payload -> localCache.invalidate(key)
						.thenCompose(ignored -> payload
								.map(this::decodeConsumedValue)
								.orElseGet(() -> CompletableFuture.completedFuture(Optional.empty()))));
	}

	@Override
	public @NotNull CompletableFuture<Page> listKeys(int page, int pageSize) {
		SynchronizationService service = serviceSupplier.get();
		if (service == null || !service.isAvailable())
			return localCache.listKeys(page, pageSize);

		return service.listKeys(name, page, pageSize);
	}

	private byte[] encodeEnvelope(byte[] payload, long expiresAt) {
		ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES + payload.length);
		buffer.putLong(expiresAt);
		buffer.put(payload);

		return buffer.array();
	}

	private Decoded decodeEnvelope(byte[] data) {
		if (data == null || data.length <= Long.BYTES)
			return null;

		ByteBuffer buffer = ByteBuffer.wrap(data);
		long expiresAt = buffer.getLong();
		byte[] payload = new byte[buffer.remaining()];
		buffer.get(payload);

		return new Decoded(payload, expiresAt);
	}

	@RequiredArgsConstructor
	private static final class Decoded {
		private final byte[] payload;
		private final long expiresAt;
	}
}
