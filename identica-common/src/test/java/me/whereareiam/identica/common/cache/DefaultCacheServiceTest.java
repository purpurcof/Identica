package me.whereareiam.identica.common.cache;

import me.whereareiam.identica.cache.Cache;
import me.whereareiam.identica.cache.codec.CacheCodec;
import me.whereareiam.identica.service.SynchronizationService;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultCacheServiceTest {
	@Test
	void localModeDoesNotUseBackend() throws Exception {
		CountingService backend = new CountingService(true);
		DefaultCacheService service = new DefaultCacheService(backend);
		Cache<String> cache = service.localCache("local", new StringCodec());

		cache.put("key", "value", 500).get(1, TimeUnit.SECONDS);
		Optional<String> value = cache.get("key").get(1, TimeUnit.SECONDS);
		assertEquals(Optional.of("value"), value);
		assertEquals(0, backend.getCalls.get());
		assertEquals(0, backend.putCalls.get());
	}

	@Test
	void synchronizedModeFallsBackWhenUnavailable() throws Exception {
		CountingService backend = new CountingService(false);
		DefaultCacheService service = new DefaultCacheService(backend);
		Cache<String> cache = service.synchronizedCache("sync", new StringCodec());

		Optional<String> value = cache.get("missing").get(1, TimeUnit.SECONDS);
		assertTrue(value.isEmpty());
		assertEquals(0, backend.getCalls.get());
	}

	private static final class StringCodec implements CacheCodec<String> {
		@Override
		public byte[] encode(String value) {
			return value.getBytes(StandardCharsets.UTF_8);
		}

		@Override
		public String decode(byte[] data) {
			return new String(data, StandardCharsets.UTF_8);
		}
	}

	private static final class CountingService implements SynchronizationService {
		private final boolean available;
		private final AtomicInteger getCalls = new AtomicInteger();
		private final AtomicInteger putCalls = new AtomicInteger();

		private CountingService(boolean available) {
			this.available = available;
		}

		@Override
		public boolean isAvailable() {
			return available;
		}

		@Override
		public @NonNull CompletableFuture<Optional<byte[]>> get(@NonNull String cacheName, @NonNull String key) {
			getCalls.incrementAndGet();
			return CompletableFuture.completedFuture(Optional.empty());
		}

		@Override
		public @NonNull CompletableFuture<Void> put(@NonNull String cacheName, @NonNull String key, byte[] value, long ttlMs) {
			putCalls.incrementAndGet();
			return CompletableFuture.completedFuture(null);
		}

		@Override
		public @NonNull CompletableFuture<Void> invalidate(@NonNull String cacheName, @NonNull String key) {
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
