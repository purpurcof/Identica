package me.whereareiam.identica.common.registry;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.actor.Identity;
import me.whereareiam.identica.common.uuid.UniqueIdResolver;
import me.whereareiam.identica.event.identity.online.IdentityOnlineAttachEvent;
import me.whereareiam.identica.event.identity.online.IdentityOnlineAttachedEvent;
import me.whereareiam.identica.event.identity.online.IdentityOnlineDetachedEvent;
import me.whereareiam.identica.event.identity.IdentityPendingEvent;
import me.whereareiam.identica.event.identity.session.IdentitySessionClosedEvent;
import me.whereareiam.identica.event.identity.session.IdentitySessionOpenEvent;
import me.whereareiam.identica.event.identity.session.IdentitySessionOpenedEvent;
import me.whereareiam.identica.model.Session;
import me.whereareiam.identica.model.auth.request.ProfileRequest;
import me.whereareiam.identica.model.config.Settings;
import me.whereareiam.identica.model.identity.IdentityState;
import me.whereareiam.identica.model.identity.IdentityState.OnlineSnapshot;
import me.whereareiam.identica.model.identity.IdentityState.PendingSnapshot;
import me.whereareiam.identica.model.identity.IdentityState.Phase;
import me.whereareiam.identica.model.identity.IdentityState.SessionSnapshot;
import me.whereareiam.identica.session.SessionDirectory;
import me.whereareiam.identica.session.SessionStore;
import me.whereareiam.identica.registry.IdentityRegistry;
import me.whereareiam.identica.util.EventUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class DefaultIdentityRegistry implements IdentityRegistry {
	private final UniqueIdResolver uniqueIdResolver;
	private final Provider<Settings> settingsProvider;
	private final SessionStore sessionStore;
	private final SessionDirectory sessionDirectory;

	private final Map<UUID, IdentityState> states = new ConcurrentHashMap<>();

	@Override
	public @Nullable UUID resolveUniqueId(@NotNull ProfileRequest request) {
		UUID uniqueId = uniqueIdResolver.resolve(request);
		if (uniqueId == null) return null;

		registerPending(uniqueId, request);
		return uniqueId;
	}

	@Override
	@NotNull
	public CompletableFuture<Optional<Session>> findSession(@Nullable UUID uniqueId) {
		return sessionStore.findByIdenticaUniqueId(uniqueId);
	}

	@Override
	@NotNull
	public CompletableFuture<@Nullable Session> openSession(@Nullable Session session) {
		if (session == null) return CompletableFuture.completedFuture(null);
		IdentitySessionOpenEvent openEvent = new IdentitySessionOpenEvent(session);
		EventUtil.callEvent(openEvent);
		if (openEvent.isCancelled())
			return CompletableFuture.completedFuture(null);

		return sessionStore.store(openEvent.getSession())
				.thenApply(stored -> {
					attachSession(stored);
					EventUtil.callEvent(new IdentitySessionOpenedEvent(stored));
					return stored;
				});
	}

	@Override
	@NotNull
	public CompletableFuture<Void> closeSession(@Nullable UUID uniqueId) {
		IdentityState state = uniqueId != null ? states.get(uniqueId) : null;
		Session current = state != null ? state.getSession() : null;

		return sessionStore.invalidateByIdenticaUniqueId(uniqueId)
				.thenRun(() -> {
					detachSession(uniqueId);
					if (uniqueId != null)
						EventUtil.callEvent(new IdentitySessionClosedEvent(uniqueId, current));
				});
	}

	@Override
	@NotNull
	public CompletableFuture<SessionDirectory.Page> listSessions(int page, int pageSize) {
		return sessionDirectory.list(page, pageSize);
	}

	@Override
	@NotNull
	public Optional<IdentityState> findState(@NotNull UUID uniqueId) {
		IdentityState state = states.get(uniqueId);

		if (state == null) return Optional.empty();
		if (state.isExpired(System.currentTimeMillis())) {
			states.remove(uniqueId, state);
			return Optional.empty();
		}

		return Optional.of(state);
	}

	@Override
	@NotNull
	public Optional<IdentityState> findState(@NotNull String username) {
		if (username.isBlank()) return Optional.empty();

		Optional<IdentityState> found = states.values()
				.stream()
				.filter(state -> username.equalsIgnoreCase(state.getUsername()))
				.findFirst();

		found.ifPresent(state -> {
			if (state.isExpired(System.currentTimeMillis()))
				states.remove(state.getUniqueId(), state);
		});

		return found.filter(state -> !state.isExpired(System.currentTimeMillis()));
	}

	@Override
	@NotNull
	public Collection<IdentityState> getStates() {
		return states.values();
	}

	@Override
	public void attachOnline(@NotNull Identity identity) {
		IdentityOnlineAttachEvent attachEvent = new IdentityOnlineAttachEvent(identity);
		EventUtil.callEvent(attachEvent);
		if (attachEvent.isCancelled())
			return;

		Identity resolved = attachEvent.getIdentity();
		states.compute(resolved.getUniqueId(), (id, state) -> {
			if (state == null) {
				return new IdentityState(
						id,
						new OnlineSnapshot(resolved, null, null, null),
						Phase.ONLINE
				);
			}

			state.transitionToOnline(resolved);
			return state;
		});

		EventUtil.callEvent(new IdentityOnlineAttachedEvent(resolved));
	}

	@Override
	public void detachOnline(@NotNull UUID uniqueId) {
		states.computeIfPresent(uniqueId, (_, state) -> {
			boolean remove = state.transitionFromOnline();
			if (remove) return null;
			return shouldRemoveState(state) ? null : state;
		});
		EventUtil.callEvent(new IdentityOnlineDetachedEvent(uniqueId));
	}

	@Override
	@NotNull
	public Optional<Identity> findOnline(@NotNull UUID uniqueId) {
		return Optional.ofNullable(states.get(uniqueId))
				.map(IdentityState::getOnlineIdentity);
	}

	@Override
	@NotNull
	public Optional<Identity> findOnline(@NotNull String username) {
		if (username.isBlank()) return Optional.empty();
		return states.values()
				.stream()
				.map(IdentityState::getOnlineIdentity)
				.filter(identity -> identity != null && identity.getUsername().equalsIgnoreCase(username))
				.findFirst();
	}

	@Override
	@NotNull
	public Collection<Identity> getOnlineIdentities() {
		return states.values()
				.stream()
				.map(IdentityState::getOnlineIdentity)
				.filter(Objects::nonNull)
				.collect(Collectors.toList());
	}

	private void registerPending(UUID uniqueId, ProfileRequest request) {
		if (uniqueId == null || request == null) return;
		cleanupExpired();

		long expiresAt = System.currentTimeMillis() + pendingTtlMillis();
		IdentityState state = states.compute(uniqueId, (id, existing) -> {
			if (existing == null) {
				PendingSnapshot snapshot = new PendingSnapshot(
						request.getUsername(),
						request.getIp(),
						request.getProviderId(),
						request.getProviderSubject(),
						expiresAt
				);
				return new IdentityState(id, snapshot, Phase.PENDING);
			}

			existing.transitionToPending(request, expiresAt);
			return existing;
		});

		if (state.getPhase() == Phase.PENDING)
			EventUtil.callEvent(new IdentityPendingEvent(state));
	}

	private void attachSession(Session session) {
		if (session == null) return;
		states.compute(session.getUniqueId(), (id, state) -> {
			if (state == null)
				return new IdentityState(id, new SessionSnapshot(session), Phase.SESSION);

			state.transitionToSession(session);
			return state;
		});
	}

	private void detachSession(UUID uniqueId) {
		if (uniqueId == null) return;
		states.computeIfPresent(uniqueId, (ignored, state) -> {
			if (state.getPhase() == Phase.SESSION)
				return null;

			if (state.getPhase() == Phase.ONLINE)
				state.clearSession();

			return shouldRemoveState(state) ? null : state;
		});
	}

	private void cleanupExpired() {
		long now = System.currentTimeMillis();
		states.entrySet().removeIf(entry -> entry.getValue().isExpired(now));
	}

	private boolean shouldRemoveState(IdentityState state) {
		if (state == null) return true;
		if (state.getPhase() == Phase.ONLINE || state.getPhase() == Phase.SESSION) return false;

		return state.isExpired(System.currentTimeMillis());
	}

	private long pendingTtlMillis() {
		return settingsProvider.get().getAuthentication().getPendingUuidTtlMinutes().toMillis();
	}
}
