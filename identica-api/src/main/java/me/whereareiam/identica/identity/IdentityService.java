package me.whereareiam.identica.identity;

import me.whereareiam.identica.identity.actor.Identity;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for tracking online identity presence.
 */
@SuppressWarnings("unused")
public interface IdentityService {
	/**
	 * Attaches an online identity to the presence store.
	 *
	 * @param identity online identity
	 */
	void attach(@NotNull Identity identity);

	/**
	 * Detaches an online identity from the presence store.
	 *
	 * @param uniqueId identity unique id
	 */
	void detach(@NotNull UUID uniqueId);

	/**
	 * Looks up an online identity by unique id.
	 *
	 * @param uniqueId identity unique id
	 * @return optional identity
	 */
	@NotNull Optional<Identity> find(@NotNull UUID uniqueId);

	/**
	 * Looks up an online identity by username (case-insensitive).
	 *
	 * @param username identity username
	 * @return optional identity
	 */
	@NotNull Optional<Identity> find(@NotNull String username);

	/**
	 * Returns all online identities.
	 *
	 * @return collection of identities
	 */
	@NotNull Collection<Identity> list();
}
