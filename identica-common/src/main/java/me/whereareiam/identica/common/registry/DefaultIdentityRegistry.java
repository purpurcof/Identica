package me.whereareiam.identica.common.registry;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.event.identity.IdentityAttachEvent;
import me.whereareiam.identica.event.identity.IdentityAttachedEvent;
import me.whereareiam.identica.event.identity.IdentityDetachedEvent;
import me.whereareiam.identica.event.identity.IdentityReservedEvent;
import me.whereareiam.identica.identity.actor.Identity;
import me.whereareiam.identica.identity.registry.IdentityExtensions;
import me.whereareiam.identica.identity.registry.IdentityRegistry;
import me.whereareiam.identica.model.Session;
import me.whereareiam.identica.model.auth.request.ProfileRequest;
import me.whereareiam.identica.model.identity.IdentityState;
import me.whereareiam.identica.model.identity.IdentityState.AuthenticatedSnapshot;
import me.whereareiam.identica.model.identity.IdentityState.OnlineSnapshot;
import me.whereareiam.identica.model.identity.IdentityState.Phase;
import me.whereareiam.identica.model.identity.IdentityState.ReservedSnapshot;
import me.whereareiam.identica.util.EventUtil;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class DefaultIdentityRegistry implements IdentityRegistry {
	private final Map<UUID, IdentityState> states = new ConcurrentHashMap<>();
	private final IdentityExtensions identityExtensions;

	@Override
	public void registerReserved(@NotNull UUID uniqueId, @NotNull ProfileRequest request, long expiresAt) {
		cleanupExpired();

		IdentityState state = states.compute(uniqueId, (id, existing) -> {
			if (existing == null) {
				ReservedSnapshot snapshot = new ReservedSnapshot(
						request.getUsername(),
						request.getIp(),
						request.getProviderId(),
						request.getProviderSubject(),
						expiresAt
				);
				return new IdentityState(id, snapshot, Phase.RESERVED);
			}

			existing.transitionToReserved(request, expiresAt);
			return existing;
		});

		if (state.getPhase() == Phase.RESERVED)
			EventUtil.callEvent(new IdentityReservedEvent(state));
	}

	@Override
	public void attachAuthenticated(@NotNull Session session) {
		states.compute(session.getUniqueId(), (id, state) -> {
			if (state == null)
				return new IdentityState(id, new AuthenticatedSnapshot(session), Phase.AUTHENTICATED);

			state.transitionToAuthenticated(session);
			return state;
		});
	}

	@Override
	public void detachAuthenticated(@NotNull UUID uniqueId) {
		states.computeIfPresent(uniqueId, (ignored, state) -> {
			if (state.getPhase() == Phase.AUTHENTICATED) {
				identityExtensions.clear(uniqueId);
				return null;
			}

			if (state.getPhase() == Phase.ONLINE)
				state.clearSession();

			if (shouldRemoveState(state)) {
				identityExtensions.clear(uniqueId);
				return null;
			}

			return state;
		});
	}

	@Override
	public @NotNull Optional<IdentityState> findState(@NotNull UUID uniqueId) {
		IdentityState state = states.get(uniqueId);

		if (state == null) return Optional.empty();
		if (state.isExpired(System.currentTimeMillis())) {
			states.remove(uniqueId, state);
			identityExtensions.clear(uniqueId);
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
				if (states.remove(state.getUniqueId(), state))
					identityExtensions.clear(state.getUniqueId());
		});

		return found.filter(state -> !state.isExpired(System.currentTimeMillis()));
	}

	@Override
	public @NotNull Collection<IdentityState> getStates() {
		return states.values();
	}

	@Override
	public void addPlayer(@NotNull Identity identity) {
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
	public void removePlayer(@NotNull UUID uniqueId) {
		states.computeIfPresent(uniqueId, (_, state) -> {
			boolean remove = state.transitionFromOnline();
			if (remove) {
				identityExtensions.clear(uniqueId);
				return null;
			}
			if (shouldRemoveState(state)) {
				identityExtensions.clear(uniqueId);
				return null;
			}
			return state;
		});
		EventUtil.callEvent(new IdentityDetachedEvent(uniqueId));
	}

	@Override
	public @NotNull Optional<Identity> findPlayer(@NotNull UUID uniqueId) {
		return Optional.ofNullable(states.get(uniqueId))
				.map(IdentityState::getOnlineIdentity);
	}

	@Override
	public @NotNull Optional<Identity> findPlayer(@NotNull String username) {
		if (username.isBlank()) return Optional.empty();
		return states.values()
				.stream()
				.map(IdentityState::getOnlineIdentity)
				.filter(identity -> identity != null && identity.getUsername().equalsIgnoreCase(username))
				.findFirst();
	}

	@Override
	public @NotNull Collection<Identity> getPlayers() {
		return states.values()
				.stream()
				.map(IdentityState::getOnlineIdentity)
				.filter(Objects::nonNull)
				.collect(Collectors.toList());
	}

	private void cleanupExpired() {
		long now = System.currentTimeMillis();
		states.entrySet().removeIf(entry -> {
			boolean expired = entry.getValue().isExpired(now);
			if (expired)
				identityExtensions.clear(entry.getKey());
			return expired;
		});
	}

	private boolean shouldRemoveState(IdentityState state) {
		if (state == null) return true;
		if (state.getPhase() == Phase.ONLINE || state.getPhase() == Phase.AUTHENTICATED) return false;

		return state.isExpired(System.currentTimeMillis());
	}
}
