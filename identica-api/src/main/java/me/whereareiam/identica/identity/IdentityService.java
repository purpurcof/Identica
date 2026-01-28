package me.whereareiam.identica.identity;

import me.whereareiam.identica.identity.actor.Identity;
import me.whereareiam.identica.model.Session;
import me.whereareiam.identica.model.account.AccountPreparation;
import me.whereareiam.identica.model.auth.request.ProfileRequest;
import me.whereareiam.identica.model.identity.IdentityState;
import me.whereareiam.identica.model.identity.provider.AccountProviderProfile;
import me.whereareiam.identica.session.SessionService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Central identity lifecycle service.
 *
 * <p>This service orchestrates identity resolution, account preparation,
 * session lifecycle, and runtime identity state updates.</p>
 */
@SuppressWarnings({"unused", "UnusedReturnvalue"})
public interface IdentityService {
	/**
	 * Resolves or reserves an Identica identity for a profile request.
	 * <p>
	 * Implementations may create a reservation when no existing identity
	 * matches the request.
	 *
	 * @param request profile request
	 * @return resolved unique id or {@code null}
	 */
	@Nullable UUID reserveIdentity(@NotNull ProfileRequest request);

	/**
	 * Clears any reservation entries for a profile request.
	 *
	 * @param request profile request
	 */
	void clearReservation(@NotNull ProfileRequest request);

	/**
	 * Prepares an account based on provider identity data.
	 *
	 * <pre>{@code
	 * AccountPreparation prep = identityService.prepareAccount(profile, existingId);
	 * if (prep.getDecision().isDenied()) {
	 *     return;
	 * }
	 * }</pre>
	 *
	 * @param profile provider profile data
	 * @param existingId existing Identica id when known
	 * @return prepared account state
	 */
	@NotNull AccountPreparation prepareAccount(
			@NotNull AccountProviderProfile profile,
			@Nullable UUID existingId
	);

	/**
	 * Opens a session and stores it.
	 *
	 * @param session session to open
	 * @return stored session or {@code null} when cancelled
	 */
	@NotNull CompletableFuture<@Nullable Session> openSession(@Nullable Session session);

	/**
	 * Closes a session by identity id.
	 *
	 * @param uniqueId identity id
	 * @return completion stage
	 */
	@NotNull CompletableFuture<Void> closeSession(@Nullable UUID uniqueId);

	/**
	 * Finds a session by identity id.
	 *
	 * @param uniqueId identity id
	 * @return optional session
	 */
	@NotNull CompletableFuture<Optional<Session>> findSession(@Nullable UUID uniqueId);

	/**
	 * Lists active sessions for the given page.
	 *
	 * @param page page number (1-based)
	 * @param pageSize number of entries per page
	 * @return page result
	 */
	@NotNull CompletableFuture<SessionService.Page> listSessions(int page, int pageSize);

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
	@NotNull Optional<Identity> findOnline(@NotNull UUID uniqueId);

	/**
	 * Gets an online identity by username.
	 * Lookup is case-insensitive.
	 *
	 * @param username identity username
	 * @return optional identity
	 */
	@NotNull Optional<Identity> findOnline(@NotNull String username);

	/**
	 * Returns all online identities.
	 *
	 * @return collection of online identities
	 */
	@NotNull Collection<Identity> getOnlineIdentities();
}
