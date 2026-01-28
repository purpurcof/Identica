package me.whereareiam.identica.common.registry;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.identity.actor.Identity;
import me.whereareiam.identica.event.identity.IdentityPendingEvent;
import me.whereareiam.identica.event.identity.IdentityAttachEvent;
import me.whereareiam.identica.event.identity.IdentityAttachedEvent;
import me.whereareiam.identica.event.identity.IdentityDetachedEvent;
import me.whereareiam.identica.model.Session;
import me.whereareiam.identica.model.auth.request.ProfileRequest;
import me.whereareiam.identica.model.identity.IdentityState;
import me.whereareiam.identica.model.identity.IdentityState.OnlineSnapshot;
import me.whereareiam.identica.model.identity.IdentityState.PendingSnapshot;
import me.whereareiam.identica.model.identity.IdentityState.Phase;
import me.whereareiam.identica.model.identity.IdentityState.SessionSnapshot;
import me.whereareiam.identica.registry.IdentityRegistry;
import me.whereareiam.identica.util.EventUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class DefaultIdentityRegistry implements IdentityRegistry {
	private final Map<UUID, IdentityState> states = new ConcurrentHashMap<>();

	@Override
	public void registerPending(@NotNull UUID uniqueId, @NotNull ProfileRequest request, long expiresAt) {
		cleanupExpired();

		IdentityState state = states.compute(uniqueId, (id, existing) -> {
			if (existing == null) {
				PendingSnapshot snapshot = new PendingSnapshot(
						request.getUsername(),
						request.getIp(),
						request.getProfileUniqueId(),
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

	@Override
	public void attachSession(@NotNull Session session) {
		states.compute(session.getUniqueId(), (id, state) -> {
			if (state == null)
				return new IdentityState(id, new SessionSnapshot(session), Phase.SESSION);

			state.transitionToSession(session);
			return state;
		});
	}

	@Override
	public void detachSession(@NotNull UUID uniqueId) {
		states.computeIfPresent(uniqueId, (ignored, state) -> {
			if (state.getPhase() == Phase.SESSION)
				return null;

			if (state.getPhase() == Phase.ONLINE)
				state.clearSession();

			return shouldRemoveState(state) ? null : state;
		});
	}

	@Override
	public @NotNull Optional<IdentityState> findState(@NotNull UUID uniqueId) {
		IdentityState state = states.get(uniqueId);

		if (state == null) return Optional.empty();
		if (state.isExpired(System.currentTimeMillis())) {
			states.remove(uniqueId, state);
			return Optional.empty();
		}

		return Optional.of(state);
	}

	@Override
	public @NotNull Optional<IdentityState> findState(@NotNull String username) {
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
	public @NotNull Collection<IdentityState> getStates() {
		return states.values();
	}

	@Override
	public void attachOnline(@NotNull Identity identity) {
		IdentityAttachEvent attachEvent = new IdentityAttachEvent(identity);
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

		EventUtil.callEvent(new IdentityAttachedEvent(resolved));
	}

	@Override
	public void detachOnline(@NotNull UUID uniqueId) {
		states.computeIfPresent(uniqueId, (_, state) -> {
			boolean remove = state.transitionFromOnline();
			if (remove) return null;
			return shouldRemoveState(state) ? null : state;
		});
		EventUtil.callEvent(new IdentityDetachedEvent(uniqueId));
	}

	@Override
	public @NotNull Optional<Identity> findOnline(@NotNull UUID uniqueId) {
		return Optional.ofNullable(states.get(uniqueId))
				.map(IdentityState::getOnlineIdentity);
	}

	@Override
	public @NotNull Optional<Identity> findOnline(@NotNull String username) {
		if (username.isBlank()) return Optional.empty();
		return states.values()
				.stream()
				.map(IdentityState::getOnlineIdentity)
				.filter(identity -> identity != null && identity.getUsername().equalsIgnoreCase(username))
				.findFirst();
	}

	@Override
	public @NotNull Collection<Identity> getOnlineIdentities() {
		return states.values()
				.stream()
				.map(IdentityState::getOnlineIdentity)
				.filter(Objects::nonNull)
				.collect(Collectors.toList());
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
}
