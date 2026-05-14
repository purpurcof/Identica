package me.whereareiam.identica.identity;

import me.whereareiam.identica.identity.actor.Identity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for tracking live online identity attachments.
 */
@SuppressWarnings("unused")
public interface IdentityService {
	/**
	 * Attaches or updates the live identity bound to a connection.
	 *
	 * @param connectionUniqueId live connection UUID
	 * @param accountUniqueId resolved account UUID, when available
	 * @param identity online identity
	 */
	void attach(
			@NotNull UUID connectionUniqueId,
			@Nullable UUID accountUniqueId,
			@NotNull Identity identity
	);

	/**
	 * Detaches the identity attached to the given live connection UUID.
	 *
	 * @param connectionUniqueId live connection UUID
	 */
	void detach(@NotNull UUID connectionUniqueId);

	/**
	 * Looks up the runtime attachment by live connection UUID.
	 *
	 * @param connectionUniqueId live connection UUID
	 * @return optional attachment
	 */
	@NotNull Optional<IdentityAttachment> findAttachmentByConnectionUniqueId(@NotNull UUID connectionUniqueId);

	/**
	 * Looks up the runtime attachment by resolved account UUID.
	 *
	 * @param accountUniqueId resolved account UUID
	 * @return optional attachment
	 */
	@NotNull Optional<IdentityAttachment> findAttachmentByAccountUniqueId(@NotNull UUID accountUniqueId);

	/**
	 * Looks up an online identity by live connection UUID.
	 *
	 * @param connectionUniqueId live connection UUID
	 * @return optional identity
	 */
	@NotNull Optional<Identity> findByConnectionUniqueId(@NotNull UUID connectionUniqueId);

	/**
	 * Looks up an online identity by resolved account UUID.
	 *
	 * @param accountUniqueId resolved account UUID
	 * @return optional identity
	 */
	@NotNull Optional<Identity> findByAccountUniqueId(@NotNull UUID accountUniqueId);

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
