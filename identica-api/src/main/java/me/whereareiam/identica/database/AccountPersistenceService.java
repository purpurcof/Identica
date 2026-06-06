package me.whereareiam.identica.database;

import me.whereareiam.identica.model.identity.Account;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for persisting and retrieving accounts.
 */
@SuppressWarnings("unused")
public interface AccountPersistenceService {
	/**
	 * Find an account by its unique id.
	 *
	 * @param uniqueId the account id
	 * @return account or empty if not found
	 */
	@NotNull Optional<Account> findByUniqueId(@NotNull UUID uniqueId);

	/**
	 * Find accounts by username.
	 *
	 * @param username account username
	 * @return matching accounts
	 */
	@NotNull List<Account> findByUsername(@NotNull String username);

	/**
	 * Counts stored account records.
	 *
	 * @return total persisted accounts
	 */
	long count();

	/**
	 * Create a new account record.
	 *
	 * @param account account to create
	 * @return created account
	 */
	@NotNull Account create(@NotNull Account account);

	/**
	 * Update the last-seen timestamp for an account.
	 *
	 * @param uniqueId account id
	 * @param lastSeenAt epoch millis
	 */
	void updateLastSeen(@NotNull UUID uniqueId, long lastSeenAt);

	/**
	 * Update the username for an account.
	 *
	 * @param uniqueId account id
	 * @param username new username
	 */
	void updateUsername(@NotNull UUID uniqueId, @NotNull String username);

	/**
	 * Delete an account record.
	 *
	 * @param uniqueId account id
	 */
	void delete(@NotNull UUID uniqueId);
}
