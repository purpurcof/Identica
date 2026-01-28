package me.whereareiam.identica.common.session;

import me.whereareiam.identica.cache.Cache;
import me.whereareiam.identica.cache.CacheService;
import me.whereareiam.identica.model.Session;
import me.whereareiam.identica.model.config.Replication;
import me.whereareiam.identica.model.config.Settings;
import me.whereareiam.identica.session.SessionService;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultSessionStoreTest {
	@Test
	void storesAndFindsByKeys() throws Exception {
		FakeCacheService cacheService = new FakeCacheService();
		SessionService service = new DefaultSessionService(
				cacheService,
				this::settings,
				this::replication
		);

		Session session = Session.builder()
				.sessionId("s1")
				.uniqueId(UUID.randomUUID())
				.providerId("premium")
				.providerSubject("subject-1")
				.originalUsername("Steve")
				.build();

		service.open(session).get(1, TimeUnit.SECONDS);

		Optional<Session> byUser = service.findByUniqueId(session.getUniqueId()).get(1, TimeUnit.SECONDS);
		Optional<Session> bySession = service.findBySessionId("s1").get(1, TimeUnit.SECONDS);
		Optional<Session> bySubject = service.findByProviderSubject("premium", "subject-1").get(1, TimeUnit.SECONDS);

		assertTrue(byUser.isPresent());
		assertTrue(bySession.isPresent());
		assertTrue(bySubject.isPresent());
		assertEquals(session.getUniqueId(), bySession.get().getUniqueId());
	}

	@Test
	void overwritesExistingSessionForSameUser() throws Exception {
		FakeCacheService cacheService = new FakeCacheService();
		SessionService service = new DefaultSessionService(
				cacheService,
				this::settings,
				this::replication
		);

		UUID identicaId = UUID.randomUUID();
		Session first = Session.builder()
				.sessionId("first")
				.uniqueId(identicaId)
				.providerId("premium")
				.providerSubject("subject-1")
				.originalUsername("Steve")
				.build();

		Session second = Session.builder()
				.sessionId("second")
				.uniqueId(identicaId)
				.providerId("premium")
				.providerSubject("subject-2")
				.originalUsername("Steve")
				.build();

		service.open(first).get(1, TimeUnit.SECONDS);
		service.open(second).get(1, TimeUnit.SECONDS);

		Optional<Session> byOldSession = service.findBySessionId("first").get(1, TimeUnit.SECONDS);
		Optional<Session> byNewSession = service.findBySessionId("second").get(1, TimeUnit.SECONDS);

		assertFalse(byOldSession.isPresent());
		assertTrue(byNewSession.isPresent());
		assertEquals("second", byNewSession.get().getSessionId());
	}

	@Test
	void refreshKeepsSessionId() throws Exception {
		FakeCacheService cacheService = new FakeCacheService();
		SessionService service = new DefaultSessionService(
				cacheService,
				this::settings,
				this::replication
		);

		UUID identicaId = UUID.randomUUID();
		Session session = Session.builder()
				.sessionId("keep")
				.uniqueId(identicaId)
				.providerId("premium")
				.providerSubject("subject-1")
				.originalUsername("Steve")
				.build();

		service.open(session).get(1, TimeUnit.SECONDS);
		service.refresh(identicaId).get(1, TimeUnit.SECONDS);

		Optional<Session> refreshed = service.findByUniqueId(identicaId).get(1, TimeUnit.SECONDS);
		assertTrue(refreshed.isPresent());
		assertEquals("keep", refreshed.get().getSessionId());
	}

	@Test
	void providerOverrideTtlApplied() throws Exception {
		FakeCacheService cacheService = new FakeCacheService();
		SessionService service = new DefaultSessionService(
				cacheService,
				this::settings,
				this::replication
		);

		UUID identicaId = UUID.randomUUID();
		Session session = Session.builder()
				.sessionId("ttl")
				.uniqueId(identicaId)
				.providerId("premium")
				.providerSubject("subject-ttl")
				.originalUsername("Steve")
				.build();

		service.open(session).get(1, TimeUnit.SECONDS);

		FakeCache userCache = cacheService.cache("identica:sessions:user");
		long ttlMs = userCache.ttlByKey.getOrDefault(identicaId.toString(), 0L);
		assertEquals(TimeUnit.MINUTES.toMillis(720), ttlMs);
	}

	private Settings settings() {
		Settings settings = new Settings();
		Settings.Sessions sessions = new Settings.Sessions();
		sessions.setDefaultTtl(Duration.ofMinutes(120));
		sessions.setRefreshTtl(Duration.ofMinutes(10));
		sessions.setProviders(Map.of("premium", Duration.ofMinutes(720)));
		settings.setSessions(sessions);
		return settings;
	}

	private Replication replication() {
		Replication replication = new Replication();
		Replication.Cache cache = new Replication.Cache();
		Replication.Sessions sessions = new Replication.Sessions();
		sessions.setUser("identica:sessions:user");
		sessions.setSession("identica:sessions:session");
		sessions.setSubject("identica:sessions:subject");
		cache.setSessions(sessions);
		replication.setCache(cache);
		return replication;
	}

	private static final class FakeCacheService implements CacheService {
		private final Map<String, Cache<Session>> caches = new HashMap<>();

		@Override
		public <T> Cache<T> localCache(String name, me.whereareiam.identica.cache.codec.CacheCodec<T> codec) {
			throw new UnsupportedOperationException();
		}

		@SuppressWarnings("unchecked")
		@Override
		public <T> Cache<T> synchronizedCache(String name, me.whereareiam.identica.cache.codec.CacheCodec<T> codec) {
			return (Cache<T>) caches.computeIfAbsent(name, ignored -> new FakeCache());
		}

		private FakeCache cache(String name) {
			return (FakeCache) caches.computeIfAbsent(name, ignored -> new FakeCache());
		}
	}

	private static final class FakeCache implements Cache<Session> {
		private final Map<String, Session> values = new HashMap<>();
		private final Map<String, Long> ttlByKey = new HashMap<>();

		@Override
		public @NonNull CompletableFuture<Optional<Session>> get(String key) {
			return CompletableFuture.completedFuture(Optional.ofNullable(values.get(key)));
		}

		@Override
		public @NonNull CompletableFuture<Void> put(String key, Session value, long ttlMs) {
			values.put(key, value);
			ttlByKey.put(key, ttlMs);
			return CompletableFuture.completedFuture(null);
		}

		@Override
		public @NonNull CompletableFuture<Void> invalidate(String key) {
			values.remove(key);
			ttlByKey.remove(key);
			return CompletableFuture.completedFuture(null);
		}
	}
}
