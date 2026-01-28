package me.whereareiam.identica.common.auth;

import me.whereareiam.identica.cache.Cache;
import me.whereareiam.identica.cache.CacheService;
import me.whereareiam.identica.common.auth.handshake.HandshakeInstructionRegistry;
import me.whereareiam.identica.model.auth.handshake.HandshakeInstruction;
import me.whereareiam.identica.model.config.Replication;
import me.whereareiam.identica.model.config.Settings;
import me.whereareiam.identica.type.HandshakeMode;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HandshakeInstructionRegistryTest {
	@Test
	void usesConfiguredTtl() {
		Settings settings = new Settings();
		Settings.Authentication authentication = new Settings.Authentication();
		authentication.setHandshakeInstructionTtl(Duration.ofMinutes(1));
		settings.setAuthentication(authentication);

		HandshakeInstructionRegistry store = new HandshakeInstructionRegistry(new FakeCacheService(), () -> settings, this::replication);
		long ttl = store.getDefaultTtlMillis();
		assertEquals(TimeUnit.MINUTES.toMillis(1), ttl);
	}

	@Test
	void requestUsesConfiguredTtl() {
		Settings settings = new Settings();
		Settings.Authentication authentication = new Settings.Authentication();
		authentication.setHandshakeInstructionTtl(Duration.ofMinutes(1));
		settings.setAuthentication(authentication);

		HandshakeInstructionRegistry store = new HandshakeInstructionRegistry(new FakeCacheService(), () -> settings, this::replication);
		long start = System.currentTimeMillis();
		store.request("Steve", HandshakeMode.ONLINE);

		Optional<HandshakeInstruction> instruction = store.peek("Steve");
		assertTrue(instruction.isPresent());

		long expiresAt = instruction.get().getExpiresAt();
		long expectedMin = start + TimeUnit.MINUTES.toMillis(1) - 1000;
		assertTrue(expiresAt >= expectedMin);
	}

	private Replication replication() {
		Replication replication = new Replication();
		Replication.Cache cache = new Replication.Cache();
		cache.setInstructions("identica:handshake-instructions");
		replication.setCache(cache);
		return replication;
	}

	private static final class FakeCacheService implements CacheService {
		private final Cache<HandshakeInstruction> cache = new FakeCache();

		@Override
		public <T> Cache<T> localCache(String name, me.whereareiam.identica.cache.codec.CacheCodec<T> codec) {
			throw new UnsupportedOperationException();
		}

		@SuppressWarnings("unchecked")
		@Override
		public <T> Cache<T> synchronizedCache(String name, me.whereareiam.identica.cache.codec.CacheCodec<T> codec) {
			return (Cache<T>) cache;
		}
	}

	private static final class FakeCache implements Cache<HandshakeInstruction> {
		private final Map<String, HandshakeInstruction> values = new HashMap<>();

		@Override
		public @NonNull CompletableFuture<Optional<HandshakeInstruction>> get(String key) {
			return CompletableFuture.completedFuture(Optional.ofNullable(values.get(key)));
		}

		@Override
		public @NonNull CompletableFuture<Void> put(String key, HandshakeInstruction value, long ttlMs) {
			values.put(key, value);
			return CompletableFuture.completedFuture(null);
		}

		@Override
		public @NonNull CompletableFuture<Void> invalidate(String key) {
			values.remove(key);
			return CompletableFuture.completedFuture(null);
		}
	}
}
