package me.whereareiam.identica.identity.registry;

import me.whereareiam.identica.identity.actor.Identity;
import me.whereareiam.identica.model.Session;
import me.whereareiam.identica.model.auth.request.ProfileRequest;
import me.whereareiam.identica.model.identity.IdentityState;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * Runtime registry that tracks identity state across reserved, authenticated, and online phases.
 */
public interface IdentityRegistry {
	/**
	 * Registers a reserved snapshot for the given identity.
	 *
	 * @param uniqueId identity unique id
	 * @param request profile request
	 * @param expiresAt expiration timestamp in millis
	 */
	void registerReserved(@NotNull UUID uniqueId, @NotNull ProfileRequest request, long expiresAt);

	/**
	 * Attaches an authenticated snapshot for the given identity.
	 *
	 * @param session session to attach
	 */
	void attachAuthenticated(@NotNull Session session);

	/**
	 * Detaches an authenticated snapshot for the given identity.
	 *
	 * @param uniqueId identity unique id
	 */
	void detachAuthenticated(@NotNull UUID uniqueId);

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
	 * Registers a player identity and transitions its state to the online phase.
	 *
	 * @param identity identity instance to register
	 */
	void addPlayer(@NotNull Identity identity);

	/**
	 * Removes a player identity and transitions its state out of the online phase.
	 *
	 * @param uniqueId identity unique id
	 */
	void removePlayer(@NotNull UUID uniqueId);

	/**
	 * Gets a player identity by unique id.
	 *
	 * @param uniqueId identity unique id
	 * @return optional identity
	 */
	@NotNull
	Optional<Identity> findPlayer(@NotNull UUID uniqueId);

	/**
	 * Gets a player identity by username.
	 * Lookup is case-insensitive.
	 *
	 * @param username identity username
	 * @return optional identity
	 */
	@NotNull
	Optional<Identity> findPlayer(@NotNull String username);

	/**
	 * Returns all player identities.
	 *
	 * @return collection of player identities
	 */
	@NotNull
	Collection<Identity> getPlayers();
}
