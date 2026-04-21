package me.whereareiam.identica.database;

import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;

/**
 * Service for durable account UUID reservations.
 */
@SuppressWarnings("unused")
public interface AccountReservationPersistenceService {
	/**
	 * Find a reserved account UUID by reservation key.
	 *
	 * @param reservationKey reservation key
	 * @return reserved UUID or empty when none exists
	 */
	@NotNull Optional<UUID> find(@NotNull String reservationKey);

	/**
	 * Store or update an account UUID reservation.
	 *
	 * @param reservationKey reservation key
	 * @param uniqueId reserved account UUID
	 * @param createdAt creation timestamp in epoch millis
	 * @param expiresAt expiration timestamp in epoch millis
	 */
	void reserve(@NotNull String reservationKey, @NotNull UUID uniqueId, long createdAt, long expiresAt);

	/**
	 * Delete a reservation by key.
	 *
	 * @param reservationKey reservation key
	 */
	void delete(@NotNull String reservationKey);

	/**
	 * Delete all reservations for an account UUID.
	 *
	 * @param uniqueId account UUID
	 */
	void deleteByUniqueId(@NotNull UUID uniqueId);

	/**
	 * Delete expired reservations.
	 *
	 * @param now current epoch millis
	 */
	void deleteExpired(long now);
}
