package me.whereareiam.identica.common.cache.type;

import me.whereareiam.identica.cache.codec.CacheCodec;
import me.whereareiam.identica.service.SynchronizationService;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SynchronizedCacheTest {
	@Test
	void usesBackendOnceThenLocal() throws Exception {
		TestBackend backend = new TestBackend();
		String key = "player";
		String value = "true";
		byte[] payload = value.getBytes(StandardCharsets.UTF_8);
		backend.putRaw(key, envelope(payload, System.currentTimeMillis() + 5000));

		SynchronizedCache<String> cache = new SynchronizedCache<>(
				"premium",
				new LocalCache<>(),
				new StringCodec(),
				() -> backend
		);

		Optional<String> first = cache.get(key).get(1, TimeUnit.SECONDS);
		assertEquals(Optional.of(value), first);
		assertEquals(1, backend.getCalls.get());

		Optional<String> second = cache.get(key).get(1, TimeUnit.SECONDS);
		assertEquals(Optional.of(value), second);
		assertEquals(1, backend.getCalls.get());
	}

	@Test
	void expiresRemoteEntryAndInvalidates() throws Exception {
		TestBackend backend = new TestBackend();
		String key = "player";
		byte[] payload = "false".getBytes(StandardCharsets.UTF_8);
		backend.putRaw(key, envelope(payload, System.currentTimeMillis() - 100));

		SynchronizedCache<String> cache = new SynchronizedCache<>(
				"premium",
				new LocalCache<>(),
				new StringCodec(),
				() -> backend
		);

		Optional<String> result = cache.get(key).get(1, TimeUnit.SECONDS);
		assertTrue(result.isEmpty());
		assertEquals(1, backend.invalidateCalls.get());
	}

	private static byte[] envelope(byte[] payload, long expiresAt) {
		ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES + payload.length);
		buffer.putLong(expiresAt);
		buffer.put(payload);

		return buffer.array();
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

	private static final class TestBackend implements SynchronizationService {
		private final ConcurrentHashMap<String, byte[]> storage = new ConcurrentHashMap<>();
		private final AtomicInteger getCalls = new AtomicInteger();
		private final AtomicInteger invalidateCalls = new AtomicInteger();

		@Override
		public boolean isAvailable() {
			return true;
		}

		@Override
		public @NonNull CompletableFuture<Optional<byte[]>> get(@NonNull String cacheName, @NonNull String key) {
			getCalls.incrementAndGet();
			return CompletableFuture.completedFuture(Optional.ofNullable(storage.get(cacheName + ":" + key)));
		}

		@Override
		public @NonNull CompletableFuture<Void> put(@NonNull String cacheName, @NonNull String key, byte[] value, long ttlMs) {
			storage.put(cacheName + ":" + key, value);
			return CompletableFuture.completedFuture(null);
		}

		@Override
		public @NonNull CompletableFuture<Void> invalidate(@NonNull String cacheName, @NonNull String key) {
			invalidateCalls.incrementAndGet();
			storage.remove(cacheName + ":" + key);
			return CompletableFuture.completedFuture(null);
		}

		@Override
		public @NonNull CompletableFuture<Void> publish(@NonNull String channel, byte[] payload) {
			return CompletableFuture.completedFuture(null);
		}

		@Override
		public void subscribe(@NonNull String channel, java.util.function.@NonNull Consumer<byte[]> handler) {
		}

		void putRaw(String key, byte[] value) {
			storage.put("premium" + ":" + key, value);
		}
	}
}

