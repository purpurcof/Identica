package me.whereareiam.identica.common.identity;

import me.whereareiam.identica.common.cache.DefaultCacheService;
import me.whereareiam.identica.identity.ReservationCache;
import me.whereareiam.identica.model.config.Replication;
import me.whereareiam.identica.service.SynchronizationService;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultReservationCacheTest {
	@Test
	void putGetInvalidate() throws Exception {
		ReservationCache store = new DefaultReservationCache(
				new DefaultCacheService(new DisabledSync()),
				this::replication
		);
		UUID uuid = UUID.randomUUID();

		store.put("key", uuid, TimeUnit.MINUTES.toMillis(1)).get(1, TimeUnit.SECONDS);
		Optional<UUID> resolved = store.get("key").get(1, TimeUnit.SECONDS);
		assertEquals(Optional.of(uuid), resolved);

		store.invalidate("key").get(1, TimeUnit.SECONDS);
		Optional<UUID> missing = store.get("key").get(1, TimeUnit.SECONDS);
		assertTrue(missing.isEmpty());
	}

	private Replication replication() {
		Replication replication = new Replication();
		Replication.Cache cache = new Replication.Cache();
		cache.setReservations("identica:reservation");
		replication.setCache(cache);
		return replication;
	}

	private static final class DisabledSync implements SynchronizationService {
		@Override
		public boolean isAvailable() {
			return false;
		}

		@Override
		public @NonNull CompletableFuture<Optional<byte[]>> get(@NonNull String namespace, @NonNull String key) {
			return CompletableFuture.completedFuture(Optional.empty());
		}

		@Override
		public @NonNull CompletableFuture<Void> put(@NonNull String namespace, @NonNull String key, byte[] value, long ttlMs) {
			return CompletableFuture.completedFuture(null);
		}

		@Override
		public @NonNull CompletableFuture<Void> invalidate(@NonNull String namespace, @NonNull String key) {
			return CompletableFuture.completedFuture(null);
		}

		@Override
		public @NonNull CompletableFuture<Void> publish(@NonNull String channel, byte[] payload) {
			return CompletableFuture.completedFuture(null);
		}

		@Override
		public void subscribe(@NonNull String channel, java.util.function.@NonNull Consumer<byte[]> handler) {
		}
	}
}

