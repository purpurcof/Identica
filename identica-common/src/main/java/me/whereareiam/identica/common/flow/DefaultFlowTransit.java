package me.whereareiam.identica.common.flow;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import me.whereareiam.identica.cache.Cache;
import me.whereareiam.identica.cache.CacheService;
import me.whereareiam.identica.flow.FlowRefeference;
import me.whereareiam.identica.flow.FlowSignal;
import me.whereareiam.identica.flow.FlowTransit;
import me.whereareiam.identica.model.config.Replication;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Singleton
public class DefaultFlowTransit implements FlowTransit {
	private final @NotNull CacheService cacheService;
	private final @NotNull String namespace;

	private final @NotNull ConcurrentMap<String, CacheEntry> caches = new ConcurrentHashMap<>();

	@Inject
	public DefaultFlowTransit(
			@NotNull CacheService cacheService,
			@NotNull Provider<Replication> replicationProvider
	) {
		this.cacheService = cacheService;
		this.namespace = resolveNamespace(replicationProvider);
	}

	@Override
	public @NotNull Scope scope(@NotNull FlowRefeference ref) {
		return new DefaultScope(ref);
	}

	private final class DefaultScope implements Scope {
		private final @NotNull FlowRefeference ref;

		private DefaultScope(@NotNull FlowRefeference ref) {
			this.ref = ref;
		}

		@Override
		public @NotNull <T> Put<T> put(@NotNull FlowSignal<T> signal, @NotNull T value) {
			return ttl -> write(signal, value, ttl);
		}

		@Override
		public @NotNull <T> Optional<T> peek(@NotNull FlowSignal<T> signal) {
			return read(signal, false);
		}

		@Override
		public @NotNull <T> Optional<T> consume(@NotNull FlowSignal<T> signal) {
			return read(signal, true);
		}

		@Override
		public void clear() {
			List<String> keys = resolveKeys(ref);
			if (keys.isEmpty())
				return;

			for (CacheEntry entry : caches.values()) {
				for (String key : keys)
					invalidateUnchecked(entry.cache, key);
			}
		}

		private <T> void write(@NotNull FlowSignal<T> signal, @NotNull T value, @Nullable Duration ttl) {
			long ttlMs = resolveTtlMs(ttl);
			if (ttlMs <= 0)
				return;

			List<String> keys = resolveKeys(ref);
			if (keys.isEmpty())
				return;

			Cache<T> cache = resolveCache(signal);
			for (String key : keys)
				cache.put(key, value, ttlMs).join();
		}

		private <T> @NotNull Optional<T> read(@NotNull FlowSignal<T> signal, boolean consume) {
			List<String> keys = resolveKeys(ref);
			if (keys.isEmpty())
				return Optional.empty();

			Cache<T> cache = resolveCache(signal);
			for (String key : keys) {
				T value = cache.get(key).join().orElse(null);
				if (value == null)
					continue;

				if (consume) {
					for (String candidate : keys)
						cache.invalidate(candidate).join();
				}

				return Optional.of(value);
			}

			return Optional.empty();
		}
	}

	@SuppressWarnings("unchecked")
	private <T> @NotNull Cache<T> resolveCache(@NotNull FlowSignal<T> signal) {
		String signalNamespace = namespace + ":" + normalizeSignal(signal.getKey());
		CacheEntry entry = caches.compute(signalNamespace, (name, existing) -> {
			String codecType = signal.getCodec().getClass().getName();
			if (existing != null && !existing.codecType.equals(codecType))
				throw new IllegalStateException("FlowSignal codec mismatch for key " + signal.getKey());

			if (existing != null)
				return existing;

			Cache<T> created = cacheService.synchronizedCache(signalNamespace, signal.getCodec());
			return new CacheEntry(created, codecType);
		});

		return (Cache<T>) entry.cache;
	}

	private void invalidateUnchecked(@NotNull Cache<?> cache, @NotNull String key) {
		@SuppressWarnings("unchecked")
		Cache<Object> typed = (Cache<Object>) cache;
		typed.invalidate(key).join();
	}

	private List<String> resolveKeys(@NotNull FlowRefeference ref) {
		List<String> keys = new ArrayList<>(3);

		if (ref.getConnectionUniqueId() != null)
			keys.add("c:" + ref.getConnectionUniqueId());

		String username = normalize(ref.getUsername());
		String ip = normalize(ref.getIp());
		if (username != null && ip != null)
			keys.add("u:" + username + "|ip:" + ip);

		if (username != null)
			keys.add("u:" + username);

		return keys;
	}

	private @Nullable String normalize(@Nullable String value) {
		if (value == null || value.isBlank())
			return null;

		return value.trim().toLowerCase(Locale.ROOT);
	}

	private String normalizeSignal(@Nullable String signalKey) {
		if (signalKey == null || signalKey.isBlank())
			throw new IllegalArgumentException("signal.key");

		StringBuilder builder = new StringBuilder(signalKey.length());
		for (char c : signalKey.trim().toCharArray()) {
			if (Character.isLetterOrDigit(c) || c == ':' || c == '.' || c == '_' || c == '-')
				builder.append(Character.toLowerCase(c));
			else
				builder.append('_');
		}
		return builder.toString();
	}

	private long resolveTtlMs(@Nullable Duration ttl) {
		if (ttl == null || ttl.isZero() || ttl.isNegative())
			return 0;

		return ttl.toMillis();
	}

	private static @NotNull String resolveNamespace(@NotNull Provider<Replication> replicationProvider) {
		Replication replication = replicationProvider.get();
		if (replication == null) throw new IllegalStateException("replication is missing");

		String resolved = replication.getCache().getPendingConnections();
		if (resolved.isBlank()) throw new IllegalStateException("replication.cache.pendingConnections is missing");

		return resolved;
	}

	private record CacheEntry(@NotNull Cache<?> cache, @NotNull String codecType) {
	}
}
