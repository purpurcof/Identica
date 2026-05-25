package me.whereareiam.identica.common.identity.session;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import me.whereareiam.identica.event.EventListener;
import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.event.base.IdenticEvent;
import me.whereareiam.identica.event.identity.session.SessionClosedEvent;
import me.whereareiam.identica.event.identity.session.SessionReplacedEvent;
import me.whereareiam.identica.event.lifecycle.IdenticaShutdownEvent;
import me.whereareiam.identica.identity.session.SessionService;
import me.whereareiam.identica.model.Session;
import me.whereareiam.identica.model.SessionCloseRequest;
import me.whereareiam.identica.model.config.provider.Providers;
import me.whereareiam.identica.model.config.Replication;
import me.whereareiam.identica.model.config.Settings;
import me.whereareiam.identica.model.replication.ReplicationType;
import me.whereareiam.identica.model.scheduler.JobKey;
import me.whereareiam.identica.model.scheduler.Origin;
import me.whereareiam.identica.model.scheduler.PeriodicalRunnableTask;
import me.whereareiam.identica.model.scheduler.Purpose;
import me.whereareiam.identica.replication.ReplicationSystem;
import me.whereareiam.identica.replication.cache.ReplicatedCache;
import me.whereareiam.identica.service.Scheduler;
import me.whereareiam.identica.type.event.EventOrder;
import me.whereareiam.identica.type.session.SessionConcurrencyPolicy;
import me.whereareiam.identica.util.UniqueIdUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Singleton
public class DefaultSessionService implements SessionService, EventListener {
	private static final Origin ORIGIN = Origin.core(DefaultSessionService.class);
	private static final Purpose PURPOSE = Purpose.of("live-session-keepalive");

	private final Provider<Settings> settingsProvider;
	private final Provider<Providers> providersProvider;
	private final Provider<Replication> replicationProvider;
	private final EventManager eventManager;
	private final Scheduler scheduler;
	private final long sessionCacheTtlMs;

	private final ReplicatedCache<Session> userCache;
	private final ReplicatedCache<Session> sessionCache;
	private final ReplicatedCache<Session> subjectCache;

	@Inject
	public DefaultSessionService(
			Provider<Settings> settingsProvider,
			Provider<Providers> providersProvider,
			EventManager eventManager,
			Scheduler scheduler,
			Provider<Replication> replicationProvider,
			ReplicationSystem replicationSystem
	) {
		this.settingsProvider = settingsProvider;
		this.providersProvider = providersProvider;
		this.replicationProvider = replicationProvider;
		this.eventManager = eventManager;
		this.scheduler = scheduler;

		Replication.Sessions sessions = resolveSessions(replicationProvider);
		long defaultTtlMs = settingsProvider.get().getConnection().getSessions().activeTtlMillis();
		ReplicationType<Session, Session> type = ReplicationType.identity(Session.class);
		this.sessionCacheTtlMs = defaultTtlMs;
		this.userCache = replicationSystem.cache(resolveNamespace(sessions.getUser(), "replication.cache.sessions.user"))
				.defaultTtl(defaultTtlMs)
				.replicated(type);
		this.sessionCache = replicationSystem.cache(resolveNamespace(sessions.getSession(), "replication.cache.sessions.session"))
				.defaultTtl(defaultTtlMs)
				.replicated(type);
		this.subjectCache = replicationSystem.cache(resolveNamespace(sessions.getSubject(), "replication.cache.sessions.subject"))
				.defaultTtl(defaultTtlMs)
				.replicated(type);
		eventManager.register(this);
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
		return close(SessionCloseRequest.builder()
				.uniqueId(uniqueId)
				.build());
	}

	@Override
	public @NotNull CompletableFuture<Void> close(@NotNull SessionCloseRequest request) {
		SessionCloseRequest prepared = prepareCloseRequest(request);

		return dispatchClose(prepared);
	}

	@Override
	public @NotNull CompletableFuture<Page> list(int page, int pageSize) {
		return userCache.listKeys(page, pageSize)
				.thenApply(keys -> {
					List<UUID> entries = new ArrayList<>();
					for (String key : keys.getEntries()) {
						UUID uniqueId = UniqueIdUtil.parseUniqueId(key);
						if (uniqueId != null) entries.add(uniqueId);
					}
					return new Page(entries, keys.getPage(), keys.getPageSize(), keys.getTotal());
				});
	}

	private CompletableFuture<Void> putAll(Session session) {
		CompletableFuture<Void> futures = userCache.put(keyUser(session.getUniqueId()), session);

		String sessionIdKey = keySession(session.getSessionId());
		if (sessionIdKey != null) {
			futures = futures.thenCompose(ignored -> sessionCache.put(sessionIdKey, session));
		}

		String subjectKey = keySubject(session.getProviderId(), session.getProviderSubject());
		if (subjectKey != null) {
			futures = futures.thenCompose(ignored -> subjectCache.put(subjectKey, session));
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

	@IdenticEvent(EventOrder.LOWEST)
	public void onSessionClosed(@NotNull SessionClosedEvent event) {
		cancelKeepalive(event.getUniqueId());
		Session session = event.getSession();
		if (session != null) {
			invalidateKeys(session).join();
			return;
		}

		userCache.invalidate(keyUser(event.getUniqueId())).join();
	}

	@IdenticEvent
	public void onShutdown(@NotNull IdenticaShutdownEvent event) {
		scheduler.cancelByOrigin(ORIGIN);
	}

	private @NotNull CompletableFuture<Void> dispatchClose(@NotNull SessionCloseRequest request) {
		UUID uniqueId = request.getUniqueId();
		return findByUniqueId(uniqueId)
				.thenAccept(existing -> eventManager.call(new SessionClosedEvent(
						uniqueId,
						existing.orElse(null),
						request
				)));
	}

	private @NotNull SessionCloseRequest prepareCloseRequest(@NotNull SessionCloseRequest request) {
		UUID requestId = request.getRequestId() != null
				? request.getRequestId()
				: UUID.randomUUID();
		String originServerId = hasText(request.getOriginServerId())
				? request.getOriginServerId()
				: resolveServerId();

		return request.toBuilder()
				.requestId(requestId)
				.originServerId(originServerId)
				.build();
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
		return cleanupForOpen(existing, incoming, policy)
				.thenCompose(ignored -> putAll(incoming))
				.thenApply(ignored -> {
					scheduleKeepalive(incoming);
					return incoming;
				})
				;
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
		Providers.ProviderEntry provider = findProvider(providerId);
		SessionConcurrencyPolicy override = provider != null
				? provider.getOverrides().getSessionConcurrencyPolicy()
				: null;
		return override != null
				? override
				: settingsProvider.get().getConnection().getSessions().getConcurrencyPolicy();
	}

	private @Nullable Providers.ProviderEntry findProvider(@Nullable String rawId) {
		String id = trimToNull(rawId);
		if (id == null) return null;

		Providers config = providersProvider.get();
		for (Providers.ProviderEntry entry : config.getProviders()) {
			if (entry == null) continue;
			String entryId = trimToNull(entry.getId());
			if (entryId != null && entryId.equalsIgnoreCase(id))
				return entry;
		}

		return null;
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

	private boolean hasText(@Nullable String value) {
		return value != null && !value.trim().isEmpty();
	}

	private @NotNull CompletableFuture<Optional<Session>> getByKey(
			@NotNull ReplicatedCache<Session> cache,
			@Nullable String key
	) {
		if (key == null)
			return CompletableFuture.completedFuture(Optional.empty());
		return cache.get(key);
	}

	private void scheduleKeepalive(@NotNull Session session) {
		UUID uniqueId = session.getUniqueId();

        long intervalMs = keepaliveIntervalMs();
		if (intervalMs <= 0)
			return;

		scheduler.schedule(PeriodicalRunnableTask.builder()
				.key(jobKey(uniqueId))
				.delay(intervalMs)
				.period(intervalMs)
				.runnable(() -> refreshLiveSession(uniqueId))
				.build());
	}

	private void refreshLiveSession(@NotNull UUID uniqueId) {
		findByUniqueId(uniqueId)
				.thenCompose(existing -> existing
						.map(this::putAll)
						.orElseGet(() -> {
							cancelKeepalive(uniqueId);
							return CompletableFuture.completedFuture(null);
						}))
				.join();
	}

	private void cancelKeepalive(@Nullable UUID uniqueId) {
		if (uniqueId == null) return;
		scheduler.cancel(jobKey(uniqueId));
	}

	private long keepaliveIntervalMs() {
		if (sessionCacheTtlMs <= 0) return 0L;
		return Math.max(1000L, sessionCacheTtlMs / 2L);
	}

	private @NotNull JobKey jobKey(@NotNull UUID uniqueId) {
		return JobKey.of(ORIGIN, PURPOSE, uniqueId.toString());
	}

	private static Replication.Sessions resolveSessions(Provider<Replication> replicationProvider) {
		Replication replication = replicationProvider.get();
		if (replication == null)
			throw new IllegalStateException("replication is missing");

		return replication.getCache().getSessions();
	}

	private @NotNull String resolveServerId() {
		Replication replication = replicationProvider.get();
		if (replication == null) return "";

		return replication.getServerId();
	}

	private static String resolveNamespace(String namespace, String label) {
		if (namespace == null || namespace.isBlank())
			throw new IllegalStateException(label + " is missing");

		return namespace;
	}
}
