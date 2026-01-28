package me.whereareiam.identica.model.identity;

import me.whereareiam.identica.identity.actor.Identity;
import me.whereareiam.identica.model.Session;
import me.whereareiam.identica.model.auth.request.ProfileRequest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Runtime identity state that bridges pre-login, session, and online phases.
 */
@SuppressWarnings("unused")
public class IdentityState {
	/**
	 * Phases of the runtime identity state.
	 */
	public enum Phase {
		PENDING,
		SESSION,
		ONLINE
	}

	/**
	 * Marker for phase-specific snapshots.
	 */
	public interface Snapshot {
	}

	/**
	 * Snapshot captured before the session is established.
	 *
	 * @param username username observed during profile rewriting
	 * @param ip ip address observed during profile rewriting
	 * @param profileUniqueId profile UUID reported by the platform
	 * @param providerId provider id reported by the platform
	 * @param providerSubject provider subject reported by the platform
	 * @param expiresAt expiration timestamp in millis
	 */
	public record PendingSnapshot(
			@Nullable String username,
			@Nullable String ip,
			@Nullable String profileUniqueId,
			@Nullable String providerId,
			@Nullable String providerSubject,
			long expiresAt
	) implements Snapshot {
	}

	/**
	 * Snapshot captured after the session is stored.
	 *
	 * @param session stored session
	 */
	public record SessionSnapshot(@NotNull Session session) implements Snapshot {
	}

	/**
	 * Snapshot captured for an online identity.
	 *
	 * @param identity online identity instance
	 * @param session attached session or {@code null}
	 * @param providerId provider id or {@code null}
	 * @param providerSubject provider subject or {@code null}
	 */
	public record OnlineSnapshot(
			@NotNull Identity identity,
			@Nullable Session session,
			@Nullable String providerId,
			@Nullable String providerSubject
	) implements Snapshot {
	}

	private final @NotNull UUID uniqueId;
	private volatile @NotNull Phase phase;
	private volatile @NotNull Snapshot snapshot;

	/**
	 * Creates an identity state for the provided unique id and snapshot.
	 *
	 * @param uniqueId identity unique id
	 * @param snapshot phase snapshot to initialize
	 * @param phase identity phase
	 */
	public IdentityState(@NotNull UUID uniqueId, @NotNull Snapshot snapshot, @NotNull Phase phase) {
		this.uniqueId = uniqueId;
		this.snapshot = snapshot;
		this.phase = phase;
	}

	/**
	 * Returns the identity unique id.
	 *
	 * @return unique id
	 */
	public @NotNull UUID getUniqueId() {
		return uniqueId;
	}

	/**
	 * Returns the current identity phase.
	 *
	 * @return phase
	 */
	public @NotNull Phase getPhase() {
		return phase;
	}

	/**
	 * Returns the current snapshot.
	 *
	 * @return snapshot
	 */
	public @NotNull Snapshot getSnapshot() {
		return snapshot;
	}

	/**
	 * Returns the last known username for this identity.
	 *
	 * @return username or {@code null}
	 */
	public @Nullable String getUsername() {
		return switch (snapshot) {
			case OnlineSnapshot online -> online.identity().getUsername();
			case SessionSnapshot(Session session) -> resolveSessionUsername(session);
			case PendingSnapshot pending -> pending.username();
			default -> null;
		};
	}

	/**
	 * Returns the last known IP address for this identity.
	 *
	 * @return IP address or {@code null}
	 */
	public @Nullable String getIp() {
		return switch (snapshot) {
			case OnlineSnapshot online -> online.identity().getIp();
			case SessionSnapshot(Session session) -> session.getIp();
			case PendingSnapshot pending -> pending.ip();
			default -> null;
		};
	}

	/**
	 * Returns the active session when available.
	 *
	 * @return session or {@code null}
	 */
	public @Nullable Session getSession() {
		if (snapshot instanceof SessionSnapshot(Session session))
			return session;
		if (snapshot instanceof OnlineSnapshot online)
			return online.session();

		return null;
	}

	/**
	 * Returns the active online identity when available.
	 *
	 * @return online identity or {@code null}
	 */
	public @Nullable Identity getOnlineIdentity() {
		if (snapshot instanceof OnlineSnapshot online)
			return online.identity();
		return null;
	}

	/**
	 * Returns the provider id associated with this identity, if known.
	 *
	 * @return provider id or {@code null}
	 */
	public @Nullable String getProviderId() {
		switch (snapshot) {
			case PendingSnapshot pending -> {
				return pending.providerId();
			}
			case SessionSnapshot(Session session) -> {
				return session.getProviderId();
			}
			case OnlineSnapshot online -> {
				String providerId = online.providerId();
				if (providerId != null && !providerId.isBlank())
					return providerId;

				Session session = online.session();
				return session != null
						? session.getProviderId()
						: null;
			}
			default -> {
			}
		}
		return null;
	}

	/**
	 * Returns the provider subject associated with this identity, if known.
	 *
	 * @return provider subject or {@code null}
	 */
	public @Nullable String getProviderSubject() {
		switch (snapshot) {
			case PendingSnapshot pending -> {
				return pending.providerSubject();
			}
			case SessionSnapshot(Session session) -> {
				return session.getProviderSubject();
			}
			case OnlineSnapshot online -> {
				String providerSubject = online.providerSubject();
				if (providerSubject != null && !providerSubject.isBlank())
					return providerSubject;
				Session session = online.session();
				return session != null ? session.getProviderSubject() : null;
			}
			default -> {
			}
		}
		return null;
	}

	/**
	 * Returns the profile UUID captured during profile rewriting, if available.
	 *
	 * @return profile UUID or {@code null}
	 */
	public @Nullable String getProfileUniqueId() {
		if (snapshot instanceof PendingSnapshot pending)
			return pending.profileUniqueId();
		return null;
	}

	/**
	 * Returns {@code true} when the state is in online phase.
	 *
	 * @return {@code true} when online identity is present
	 */
	public boolean isOnline() {
		return phase == Phase.ONLINE;
	}

	/**
	 * Returns whether this state has expired without an active session or online identity.
	 *
	 * @param nowMillis current time in millis
	 * @return {@code true} when the state is expired
	 */
	public boolean isExpired(long nowMillis) {
		if (phase != Phase.PENDING) return false;
		PendingSnapshot pending = (PendingSnapshot) snapshot;
		return pending.expiresAt() > 0 && nowMillis >= pending.expiresAt();
	}

	/**
	 * Transitions the state into the pending phase based on a profile request.
	 *
	 * @param request profile request
	 * @param expiresAt expiration timestamp in millis
	 */
	public synchronized void transitionToPending(@NotNull ProfileRequest request, long expiresAt) {
		if (phase == Phase.ONLINE || phase == Phase.SESSION)
			return;

		String username = request.getUsername();
		String ip = request.getIp();
		this.snapshot = new PendingSnapshot(
				username,
				ip,
				request.getProfileUniqueId(),
				request.getProviderId(),
				request.getProviderSubject(),
				expiresAt
		);
		this.phase = Phase.PENDING;
	}

	/**
	 * Transitions the state into the online phase for the provided identity.
	 *
	 * @param identity online identity
	 */
	public synchronized void transitionToOnline(@NotNull Identity identity) {
		Identity current = getOnlineIdentity();
		if (current != null && current != identity)
			identity.syncMetadataFrom(current);

		Session session = getSession();
		String providerId = getProviderId();
		String providerSubject = getProviderSubject();

		this.snapshot = new OnlineSnapshot(identity, session, providerId, providerSubject);
		this.phase = Phase.ONLINE;
	}

	/**
	 * Transitions the state out of the online phase.
	 *
	 * @return {@code true} when the state should be removed
	 */
	public synchronized boolean transitionFromOnline() {
		if (phase != Phase.ONLINE)
			return false;

		OnlineSnapshot online = (OnlineSnapshot) snapshot;
		if (online.session() != null) {
			this.snapshot = new SessionSnapshot(online.session());
			this.phase = Phase.SESSION;
			return false;
		}

		return true;
	}

	/**
	 * Clears the attached session while keeping the online phase.
	 */
	public synchronized void clearSession() {
		if (phase != Phase.ONLINE || !(snapshot instanceof OnlineSnapshot online))
			return;

		String providerId = getProviderId();
		String providerSubject = getProviderSubject();
		this.snapshot = new OnlineSnapshot(online.identity(), null, providerId, providerSubject);
		this.phase = Phase.ONLINE;
	}

	/**
	 * Transitions the state into the session phase.
	 *
	 * @param session session to attach
	 */
	public synchronized void transitionToSession(@NotNull Session session) {
		if (phase == Phase.ONLINE && snapshot instanceof OnlineSnapshot online) {
			String providerId = session.getProviderId();
			String providerSubject = session.getProviderSubject();
			this.snapshot = new OnlineSnapshot(online.identity(), session, providerId, providerSubject);
			this.phase = Phase.ONLINE;
			return;
		}

		this.snapshot = new SessionSnapshot(session);
		this.phase = Phase.SESSION;
	}

	private @Nullable String resolveSessionUsername(@Nullable Session session) {
		if (session == null) return null;
		String effective = session.getEffectiveUsername();
		if (effective != null && !effective.isBlank())
			return effective;
		return session.getOriginalUsername();
	}
}
