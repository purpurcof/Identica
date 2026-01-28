package me.whereareiam.identica.common.identity;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import me.whereareiam.identica.cache.Cache;
import me.whereareiam.identica.cache.CacheService;
import me.whereareiam.identica.cache.codec.type.StringCodec;
import me.whereareiam.identica.identity.ReservationCache;
import me.whereareiam.identica.model.config.Replication;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Singleton
public class DefaultReservationCache implements ReservationCache {
	private final Cache<String> cache;

	@Inject
	public DefaultReservationCache(
			CacheService cacheService,
			Provider<Replication> replicationProvider
	) {
		this.cache = cacheService.synchronizedCache(resolveNamespace(replicationProvider), new StringCodec());
	}

	@Override
	public @NotNull CompletableFuture<Optional<UUID>> get(String key) {
		if (key == null || key.isBlank()) {
			return CompletableFuture.completedFuture(Optional.empty());
		}
		return cache.get(key)
				.thenApply(value -> value.flatMap(DefaultReservationCache::parse));
	}

	@Override
	public @NotNull CompletableFuture<Void> put(String key, UUID uuid, long ttlMs) {
		if (key == null || key.isBlank() || uuid == null) {
			return CompletableFuture.completedFuture(null);
		}
		return cache.put(key, uuid.toString(), ttlMs);
	}

	@Override
	public @NotNull CompletableFuture<Void> invalidate(String key) {
		if (key == null || key.isBlank()) {
			return CompletableFuture.completedFuture(null);
		}
		return cache.invalidate(key);
	}

	private static Optional<UUID> parse(String value) {
		if (value == null || value.isBlank()) return Optional.empty();
		try {
			return Optional.of(UUID.fromString(value));
		} catch (IllegalArgumentException ignored) {
			return Optional.empty();
		}
	}

	private static String resolveNamespace(Provider<Replication> replicationProvider) {
		Replication replication = replicationProvider.get();
		if (replication == null)
			throw new IllegalStateException("replication is missing");

		String namespace = replication.getCache().getReservations();
		if (namespace.isBlank())
			throw new IllegalStateException("replication.cache.reservations is missing");

		return namespace;
	}
}
