package me.whereareiam.identica.common.identity.session;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import me.whereareiam.identica.replication.cache.ReplicatedCache;
import me.whereareiam.identica.replication.ReplicationSystem;
import me.whereareiam.identica.model.replication.ReplicationType;
import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.event.identity.session.SessionReplacedEvent;
import me.whereareiam.identica.model.Session;
import me.whereareiam.identica.model.config.Settings;
import me.whereareiam.identica.model.config.Replication;
import me.whereareiam.identica.identity.session.SessionService;
import me.whereareiam.identica.type.session.SessionConcurrencyPolicy;
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
	private final Provider<Settings> settingsProvider;
	private final EventManager eventManager;

	private final ReplicatedCache<Session> userCache;
	private final ReplicatedCache<Session> sessionCache;
	private final ReplicatedCache<Session> subjectCache;

	@Inject
	public DefaultSessionService(
			Provider<Settings> settingsProvider,
			EventManager eventManager,
			Provider<Replication> replicationProvider,
			ReplicationSystem replicationSystem
	) {
		this.settingsProvider = settingsProvider;
		this.eventManager = eventManager;

		Replication.Sessions sessions = resolveSessions(replicationProvider);
		ReplicationType<Session, Session> type = ReplicationType.identity(Session.class);
		this.userCache = replicationSystem.cache(resolveNamespace(sessions.getUser(), "replication.cache.sessions.user")).replicated(type);
		this.sessionCache = replicationSystem.cache(resolveNamespace(sessions.getSession(), "replication.cache.sessions.session")).replicated(type);
		this.subjectCache = replicationSystem.cache(resolveNamespace(sessions.getSubject(), "replication.cache.sessions.subject")).replicated(type);
	}

	@Override
	public @NotNull CompletableFuture<Optional<Session>> findBySessionId(@Nullable String sessionId) {
		return getByKey(sessionCache, keySession(sessionId));
	}

	@Override
	public @NotNull CompletableFuture<Optional<Session>> findByUniqueId(@Nullable UUID uniqueId) {
		return getByKey(userCache, keyUser(uniqueId));
	}

	@Override
	public @NotNull CompletableFuture<Optional<Session>> findByProviderSubject(
			@Nullable String providerId,
			@Nullable String providerSubject
	) {
		return getByKey(subjectCache, keySubject(providerId, providerSubject));
	}

	@Override
	public @NotNull CompletableFuture<@Nullable Session> open(@Nullable Session session) {
		if (session == null)
			return CompletableFuture.completedFuture(null);
		SessionConcurrencyPolicy policy = resolveConcurrencyPolicy(session.getProviderId());
		return open(session, policy);
	}

	@Override
	public @NotNull CompletableFuture<@Nullable Session> open(
			@Nullable Session session,
			@NotNull SessionConcurrencyPolicy policy
	) {
		if (session == null)
			return CompletableFuture.completedFuture(null);
		return findByUniqueId(session.getUniqueId())
				.thenCompose(existingOptional -> openWithExisting(session, existingOptional.orElse(null), policy));
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
					for (String key : keys.getEntries()) {
						UUID uniqueId = parseUniqueId(key);
						if (uniqueId != null) entries.add(uniqueId);
					}
					return new Page(entries, keys.getPage(), keys.getPageSize(), keys.getTotal());
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

	private @NotNull CompletableFuture<@Nullable Session> openWithExisting(
			@NotNull Session incoming,
			@Nullable Session existing,
			@NotNull SessionConcurrencyPolicy policy
	) {
		incoming.adoptSessionIdFrom(existing);
		if (shouldRejectIncoming(existing, incoming, policy))
			return CompletableFuture.completedFuture(null);

		prepareSession(incoming);
		long ttlMs = resolveTtl(incoming.getProviderId()).toMillis();
		return cleanupForOpen(existing, incoming, policy)
				.thenCompose(ignored -> putAll(incoming, ttlMs))
				.thenApply(ignored -> incoming);
	}

	private boolean shouldRejectIncoming(
			@Nullable Session existing,
			@NotNull Session incoming,
			@NotNull SessionConcurrencyPolicy policy
	) {
		return existing != null && !sameSession(existing, incoming) && policy.rejectsNew();
	}

	private @NotNull CompletableFuture<Void> cleanupForOpen(
			@Nullable Session existing,
			@NotNull Session incoming,
			@NotNull SessionConcurrencyPolicy policy
	) {
		if (existing == null || sameSession(existing, incoming))
			return CompletableFuture.completedFuture(null);

		if (policy.replacesExisting()) {
			eventManager.call(new SessionReplacedEvent(existing, incoming));
		}

		return invalidateKeys(existing);
	}

	private SessionConcurrencyPolicy resolveConcurrencyPolicy(@Nullable String providerId) {
		Settings.Sessions sessions = settingsProvider.get().getConnection().getSessions();
		SessionConcurrencyPolicy policy = sessions.getConcurrencyPolicy();
		SessionConcurrencyPolicy override = findOverride(sessions.getConcurrencyOverrides(), providerId);

		return override != null ? override : policy;
	}

	private Duration resolveTtl(String providerId) {
		Settings.Sessions sessions = settingsProvider.get().getConnection().getSessions();

		Duration resolved = requireDuration(sessions.getDefaultTtl(), "settings.connection.sessions.defaultTtl");
		Duration override = findOverride(sessions.getProviders(), providerId);

		if (override != null) return requireDuration(override, "settings.connection.sessions.providers." + providerId);

		return resolved;
	}

	private <T> @Nullable T findOverride(@NotNull Map<String, T> overrides, @Nullable String rawKey) {
		String key = trimToNull(rawKey);
		if (key == null)
			return null;

		T exact = overrides.get(rawKey);
		if (exact != null)
			return exact;

		T trimmed = overrides.get(key);
		if (trimmed != null)
			return trimmed;

		return overrides.get(key.toLowerCase());
	}

	private Duration requireDuration(Duration duration, String key) {
		if (duration == null)
			throw new IllegalStateException(key + " is missing");
		if (duration.isZero() || duration.isNegative())
			throw new IllegalStateException(key + " must be positive");

		return duration;
	}

	private String keySession(String sessionId) {
		return trimToNull(sessionId);
	}

	private String keyUser(UUID uniqueId) {
		if (uniqueId == null) return null;
		return uniqueId.toString();
	}

	private String keySubject(String providerId, String providerSubject) {
		String normalizedProviderId = normalize(providerId);
		String normalizedProviderSubject = normalize(providerSubject);
		if (normalizedProviderId == null || normalizedProviderSubject == null)
			return null;
		return normalizedProviderId + ":" + normalizedProviderSubject;
	}

	private UUID parseUniqueId(String value) {
		String trimmed = trimToNull(value);
		if (trimmed == null) return null;

		try {
			return UUID.fromString(trimmed);
		} catch (IllegalArgumentException ignored) {
			return null;
		}
	}

	private @Nullable String normalize(@Nullable String value) {
		String trimmed = trimToNull(value);
		return trimmed != null
				? trimmed.toLowerCase()
				: null;
	}

	private @Nullable String trimToNull(@Nullable String value) {
		if (value == null)
			return null;
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private @NotNull CompletableFuture<Optional<Session>> getByKey(
			@NotNull ReplicatedCache<Session> cache,
			@Nullable String key
	) {
		if (key == null)
			return CompletableFuture.completedFuture(Optional.empty());
		return cache.get(key);
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
