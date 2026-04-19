package me.whereareiam.identica.engine.pipeline;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import me.whereareiam.identica.event.pipeline.state.PipelineStateClearedEvent;
import me.whereareiam.identica.event.pipeline.state.PipelineStateSavedEvent;
import me.whereareiam.identica.model.config.Replication;
import me.whereareiam.identica.model.pipeline.state.PipelineState;
import me.whereareiam.identica.model.replication.ReplicationType;
import me.whereareiam.identica.logging.Logger;
import me.whereareiam.identica.pipeline.state.PipelineStateStore;
import me.whereareiam.identica.model.pipeline.state.PipelineStateReference;
import me.whereareiam.identica.replication.ReplicationSystem;
import me.whereareiam.identica.replication.cache.ReplicatedCache;
import me.whereareiam.identica.util.EventUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Singleton
public class DefaultPipelineStateStore implements PipelineStateStore {
	private static final String KEY_CONNECTION_ID_PREFIX = "c:";
	private static final String KEY_IDENTITY_ID_PREFIX = "i:";
	private static final String KEY_CONNECTION_KEY_PREFIX = "k:";

	private final ReplicatedCache<PipelineStateSnapshot> stateCache;

	@Inject
	public DefaultPipelineStateStore(
			@NotNull ReplicationSystem replicationSystem,
			@NotNull Provider<Replication> replicationProvider
	) {
		ReplicationType<PipelineStateSnapshot, PipelineStateSnapshot> type = ReplicationType.identity(PipelineStateSnapshot.class);
		this.stateCache = replicationSystem.cache(resolveNamespace(replicationProvider)).replicated(type);
	}

	@Override
	public @NotNull Optional<PipelineState> find(@NotNull PipelineStateReference reference) {
		return read(reference, false);
	}

	@Override
	public @NotNull PipelineState load(@NotNull PipelineStateReference reference) {
		return find(reference).orElseGet(PipelineState::initial);
	}

	@Override
	public void save(@NotNull PipelineStateReference reference, @NotNull PipelineState state, long ttlMs) {
		if (ttlMs <= 0 || reference.isEmpty())
			return;

		long expiresAt = System.currentTimeMillis() + ttlMs;
		List<String> keys = resolveKeys(reference);
		if (keys.isEmpty())
			return;

		PipelineStateSnapshot stored = new PipelineStateSnapshot(state, keys, expiresAt);
		invalidateOverlappingSnapshots(keys, stored);
		for (String key : keys)
			stateCache.put(key, stored, ttlMs).join();

		EventUtil.callEvent(new PipelineStateSavedEvent(reference, state, expiresAt));
	}

	@Override
	public @NotNull Optional<PipelineState> consume(@NotNull PipelineStateReference reference) {
		Optional<PipelineState> resolved = read(reference, true);
		resolved.ifPresent(state -> EventUtil.callEvent(new PipelineStateClearedEvent(reference, state)));
		return resolved;
	}

	@Override
	public void clear(@NotNull PipelineStateReference reference) {
		Optional<PipelineState> resolved = read(reference, true);
		resolved.ifPresent(state -> EventUtil.callEvent(new PipelineStateClearedEvent(reference, state)));
	}

	private @NotNull Optional<PipelineState> read(
			@NotNull PipelineStateReference reference,
			boolean consume
	) {
		if (reference.isEmpty())
			return Optional.empty();

		List<String> keys = resolveKeys(reference);
		if (keys.isEmpty())
			return Optional.empty();

		long now = System.currentTimeMillis();
		for (String key : keys) {
			PipelineStateSnapshot stored = readStored(key, consume);
			if (stored == null)
				continue;

			if (stored.expiresAt > 0 && stored.expiresAt <= now) {
				invalidateKeys(stored.keys);
				continue;
			}

			PipelineState resolved = stored.state != null
					? stored.state.pruneExpired(now)
					: null;

			if (consume)
				invalidateKeys(stored.keys);

			Logger.debug(
					"Pipeline state %s hit key=%s keys=%s pipeline=%s expiresAt=%s",
					consume ? "consume" : "find",
					key,
					keys,
					resolved != null ? resolved.getPipelineType() : null,
					stored.expiresAt
			);
			return Optional.ofNullable(resolved);
		}

		Logger.debug(
				"Pipeline state %s miss connection=%s identity=%s key=%s candidates=%s",
				consume ? "consume" : "find",
				reference.getConnectionUniqueId(),
				reference.getIdentityUniqueId(),
				reference.getConnectionKey(),
				keys
		);
		return Optional.empty();
	}

	private @Nullable DefaultPipelineStateStore.PipelineStateSnapshot readStored(@NotNull String key, boolean consume) {
		return consume
				? stateCache.consume(key).join().orElse(null)
				: stateCache.getFresh(key).join().orElse(null);
	}

	private void invalidateOverlappingSnapshots(
			@NotNull List<String> keys,
			@NotNull PipelineStateSnapshot next
	) {
		for (String key : keys) {
			PipelineStateSnapshot existing = readStored(key, false);
			if (existing == null || sameKeySet(existing.keys, next.keys))
				continue;

			invalidateKeys(existing.keys);
		}
	}

	private void invalidateKeys(@Nullable List<String> keys) {
		if (keys == null || keys.isEmpty())
			return;
		for (String key : keys)
			stateCache.invalidate(key).join();
	}

	private @NotNull List<String> resolveKeys(@NotNull PipelineStateReference reference) {
		List<String> keys = new ArrayList<>(3);

		if (reference.getConnectionUniqueId() != null)
			keys.add(KEY_CONNECTION_ID_PREFIX + reference.getConnectionUniqueId());

		if (reference.getIdentityUniqueId() != null)
			keys.add(KEY_IDENTITY_ID_PREFIX + reference.getIdentityUniqueId());

		String connectionKey = reference.getConnectionKey();
		if (connectionKey != null && !connectionKey.isBlank()) {
			keys.add(KEY_CONNECTION_KEY_PREFIX + connectionKey);
			String originAgnosticConnectionKey = resolveOriginAgnosticConnectionKey(connectionKey);
			if (originAgnosticConnectionKey != null && !originAgnosticConnectionKey.equals(connectionKey))
				keys.add(KEY_CONNECTION_KEY_PREFIX + originAgnosticConnectionKey);
		}

		return keys;
	}

	private boolean sameKeySet(@Nullable List<String> left, @Nullable List<String> right) {
		if (left == null || right == null)
			return left == right;

		Set<String> leftKeys = new LinkedHashSet<>(left);
		Set<String> rightKeys = new LinkedHashSet<>(right);
		return leftKeys.equals(rightKeys);
	}

	private @Nullable String resolveOriginAgnosticConnectionKey(@NotNull String connectionKey) {
		String[] parts = connectionKey.split("\\|", -1);
		if (parts.length != 4)
			return null;

		if (parts[2].isBlank() && parts[3].isBlank())
			return connectionKey;

		return String.join("|", parts[0], parts[1], "", "");
	}

	private static @NotNull String resolveNamespace(@NotNull Provider<Replication> replicationProvider) {
		Replication replication = replicationProvider.get();
		if (replication == null) throw new IllegalStateException("replication is missing");

		String resolved = replication.getCache().getPipelineState();
		if (resolved.isBlank()) throw new IllegalStateException("replication.cache.pipelineState is missing");

		return resolved;
	}

	@NoArgsConstructor
	@AllArgsConstructor
	public static final class PipelineStateSnapshot {
		public @Nullable PipelineState state;
		public @Nullable List<String> keys;
		public long expiresAt;
	}
}
