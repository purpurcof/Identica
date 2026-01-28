package me.whereareiam.identica.common.session;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import me.whereareiam.identica.cache.Cache;
import me.whereareiam.identica.cache.CacheService;
import me.whereareiam.identica.cache.codec.type.JsonCodec;
import me.whereareiam.identica.model.Session;
import me.whereareiam.identica.model.config.Replication;
import me.whereareiam.identica.model.config.Settings;
import me.whereareiam.identica.session.SessionService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Singleton
public class DefaultSessionService implements SessionService {
	private final Cache<Session> userCache;
	private final Cache<Session> sessionCache;
	private final Cache<Session> subjectCache;
	private final Provider<Settings> settingsProvider;

	@Inject
	public DefaultSessionService(
			CacheService cacheService,
			Provider<Settings> settingsProvider,
			Provider<Replication> replicationProvider
	) {
		Replication.Sessions sessions = resolveSessions(replicationProvider);
		this.userCache = cacheService.synchronizedCache(resolveNamespace(sessions.getUser(), "replication.cache.sessions.user"), JsonCodec.of(Session.class));
		this.sessionCache = cacheService.synchronizedCache(resolveNamespace(sessions.getSession(), "replication.cache.sessions.session"), JsonCodec.of(Session.class));
		this.subjectCache = cacheService.synchronizedCache(resolveNamespace(sessions.getSubject(), "replication.cache.sessions.subject"), JsonCodec.of(Session.class));
		this.settingsProvider = settingsProvider;
	}

	@Override
	public @NotNull CompletableFuture<Optional<Session>> findBySessionId(@Nullable String sessionId) {
		String key = keySession(sessionId);
		if (key == null) return CompletableFuture.completedFuture(Optional.empty());
		return sessionCache.get(key);
	}

	@Override
	public @NotNull CompletableFuture<Optional<Session>> findByUniqueId(@Nullable UUID uniqueId) {
		String key = keyUser(uniqueId);
		if (key == null) return CompletableFuture.completedFuture(Optional.empty());
		return userCache.get(key);
	}

	@Override
	public @NotNull CompletableFuture<Optional<Session>> findByProviderSubject(
			@Nullable String providerId,
			@Nullable String providerSubject
	) {
		String key = keySubject(providerId, providerSubject);
		if (key == null) return CompletableFuture.completedFuture(Optional.empty());
		return subjectCache.get(key);
	}

	@Override
	public @NotNull CompletableFuture<@Nullable Session> open(@Nullable Session session) {
		if (session == null) {
			return CompletableFuture.completedFuture(session);
		}

		return findByUniqueId(session.getUniqueId())
				.thenCompose(existing -> {
					CompletableFuture<Void> cleanup = CompletableFuture.completedFuture(null);
					if (existing.isPresent() && !sameSession(existing.get(), session)) {
						cleanup = invalidateKeys(existing.get());
					}

					prepareSession(session);
					Duration ttl = resolveTtl(session.getProviderId());
					long ttlMs = ttl.toMillis();

					return cleanup.thenCompose(ignored -> putAll(session, ttlMs))
							.thenApply(ignored -> session);
				});
	}

	@Override
	public @NotNull CompletableFuture<Void> close(@Nullable UUID uniqueId) {
		if (uniqueId == null) return CompletableFuture.completedFuture(null);
		return findByUniqueId(uniqueId)
				.thenCompose(existing -> existing.map(this::invalidateKeys).orElseGet(() -> userCache.invalidate(keyUser(uniqueId))));
	}

	@Override
	public @NotNull CompletableFuture<Void> refresh(@Nullable UUID uniqueId) {
		if (uniqueId == null) return CompletableFuture.completedFuture(null);
		return findByUniqueId(uniqueId)
				.thenCompose(existing -> existing.<CompletionStage<Void>>map(session -> open(session)
						.thenApply(ignored -> null))
						.orElseGet(() -> CompletableFuture.completedFuture(null)));
	}

	@Override
	public @NotNull CompletableFuture<Page> list(int page, int pageSize) {
		return userCache.listKeys(page, pageSize)
				.thenApply(keys -> {
					List<UUID> entries = new ArrayList<>();
					for (String key : keys.entries()) {
						UUID uniqueId = parseUuid(key);
						if (uniqueId != null) entries.add(uniqueId);
					}
					return new Page(entries, keys.page(), keys.pageSize(), keys.total());
				});
	}

	private CompletableFuture<Void> putAll(Session session, long ttlMs) {
		CompletableFuture<Void> futures = userCache.put(keyUser(session.getUniqueId()), session, ttlMs);

		String sessionIdKey = keySession(session.getSessionId());
		if (sessionIdKey != null) {
			futures = futures.thenCompose(ignored -> sessionCache.put(sessionIdKey, session, ttlMs));
		}

		String subjectKey = keySubject(session.getProviderId(), session.getProviderSubject());
		if (subjectKey != null) {
			futures = futures.thenCompose(ignored -> subjectCache.put(subjectKey, session, ttlMs));
		}

		return futures;
	}

	private CompletableFuture<Void> invalidateKeys(Session session) {
		CompletableFuture<Void> futures = userCache.invalidate(keyUser(session.getUniqueId()));

		String sessionIdKey = keySession(session.getSessionId());
		if (sessionIdKey != null) {
			futures = futures.thenCompose(ignored -> sessionCache.invalidate(sessionIdKey));
		}

		String subjectKey = keySubject(session.getProviderId(), session.getProviderSubject());
		if (subjectKey != null) {
			futures = futures.thenCompose(ignored -> subjectCache.invalidate(subjectKey));
		}

		return futures;
	}

	private void prepareSession(Session session) {
		if (session.getSessionId() == null || session.getSessionId().isBlank()) {
			session.setSessionId(UUID.randomUUID().toString());
		}
		if (session.getCreatedAt() <= 0) {
			session.setCreatedAt(System.currentTimeMillis());
		}
		if (session.getEffectiveUsername() == null || session.getEffectiveUsername().isBlank()) {
			session.setEffectiveUsername(session.getOriginalUsername());
		}
	}

	private boolean sameSession(Session existing, Session incoming) {
		if (existing == null || incoming == null) return false;
		String existingId = existing.getSessionId();
		String incomingId = incoming.getSessionId();
		return existingId != null && existingId.equals(incomingId);
	}

	private Duration resolveTtl(String providerId) {
		Settings.Sessions sessions = settingsProvider.get().getSessions();

		Duration resolved = requireDuration(sessions.getDefaultTtl(), "settings.sessions.defaultTtl");
		Map<String, Duration> overrides = sessions.getProviders();
		if (providerId == null || providerId.isBlank())
			return resolved;

		Duration override = overrides.get(providerId);
		if (override == null) {
			override = overrides.get(providerId.trim());
		}
		if (override == null) {
			override = overrides.get(providerId.trim().toLowerCase());
		}

		if (override != null)
			return requireDuration(override, "settings.sessions.providers." + providerId);

		return resolved;
	}

	private Duration requireDuration(Duration duration, String key) {
		if (duration == null)
			throw new IllegalStateException(key + " is missing");
		if (duration.isZero() || duration.isNegative())
			throw new IllegalStateException(key + " must be positive");

		return duration;
	}

	private String keySession(String sessionId) {
		if (sessionId == null || sessionId.isBlank()) return null;
		return sessionId.trim();
	}

	private String keyUser(UUID uniqueId) {
		if (uniqueId == null) return null;
		return uniqueId.toString();
	}

	private String keySubject(String providerId, String providerSubject) {
		if (providerId == null || providerId.isBlank()) return null;
		if (providerSubject == null || providerSubject.isBlank()) return null;
		return normalize(providerId) + ":" + normalize(providerSubject);
	}

	private UUID parseUuid(String value) {
		if (value == null || value.isBlank()) return null;
		try {
			return UUID.fromString(value.trim());
		} catch (IllegalArgumentException ignored) {
			return null;
		}
	}

	private String normalize(String value) {
		return value.trim().toLowerCase();
	}

	private static Replication.Sessions resolveSessions(Provider<Replication> replicationProvider) {
		Replication replication = replicationProvider.get();
		if (replication == null)
			throw new IllegalStateException("replication is missing");

		return replication.getCache().getSessions();
	}

	private static String resolveNamespace(String namespace, String label) {
		if (namespace == null || namespace.isBlank())
			throw new IllegalStateException(label + " is missing");
		return namespace;
	}
}
