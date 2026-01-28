package me.whereareiam.identica.registry;

import me.whereareiam.identica.identity.actor.Identity;
import me.whereareiam.identica.model.Session;
import me.whereareiam.identica.model.auth.request.ProfileRequest;
import me.whereareiam.identica.model.identity.IdentityState;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * Runtime registry that tracks identity state across pending, session, and online phases.
 */
public interface IdentityRegistry {
	/**
	 * Registers a pending snapshot for the given identity.
	 *
	 * @param uniqueId identity unique id
	 * @param request profile request
	 * @param expiresAt expiration timestamp in millis
	 */
	void registerPending(@NotNull UUID uniqueId, @NotNull ProfileRequest request, long expiresAt);

	/**
	 * Attaches a session snapshot for the given identity.
	 *
	 * @param session session to attach
	 */
	void attachSession(@NotNull Session session);

	/**
	 * Detaches a session snapshot for the given identity.
	 *
	 * @param uniqueId identity unique id
	 */
	void detachSession(@NotNull UUID uniqueId);

	/**
	 * Returns the identity state for the given unique id, if present.
	 *
	 * @param uniqueId identity unique id
	 * @return optional identity state
	 */
	@NotNull Optional<IdentityState> findState(@NotNull UUID uniqueId);

	/**
	 * Returns the identity state for the given username, if present.
	 * Lookup is case-insensitive.
	 *
	 * @param username identity username
	 * @return optional identity state
	 */
	@NotNull Optional<IdentityState> findState(@NotNull String username);

	/**
	 * Returns all tracked identity states.
	 *
	 * @return collection of identity states
	 */
	@NotNull Collection<IdentityState> getStates();

	/**
	 * Transitions the state into the online phase for the provided identity.
	 *
	 * @param identity identity instance to attach
	 */
	void attachOnline(@NotNull Identity identity);

	/**
	 * Transitions the state out of the online phase for the provided identity id.
	 *
	 * @param uniqueId identity unique id
	 */
	void detachOnline(@NotNull UUID uniqueId);

	/**
	 * Gets an online identity by unique id.
	 *
	 * @param uniqueId identity unique id
	 * @return optional identity
	 */
	@NotNull
	Optional<Identity> findOnline(@NotNull UUID uniqueId);

	/**
	 * Gets an online identity by username.
	 * Lookup is case-insensitive.
	 *
	 * @param username identity username
	 * @return optional identity
	 */
	@NotNull
	Optional<Identity> findOnline(@NotNull String username);

	/**
	 * Returns all online identities.
	 *
	 * @return collection of online identities
	 */
	@NotNull
	Collection<Identity> getOnlineIdentities();
}
