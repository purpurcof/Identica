package me.whereareiam.identica.database;

import me.whereareiam.identica.model.Account;

import java.util.Optional;
import java.util.UUID;

/**
 * Service for persisting and retrieving account data.
 */
@SuppressWarnings("unused")
public interface AccountPersistenceService {
	/**
	 * Find an account by its unique identifier.
	 *
	 * @param uniqueId the Identica account unique id
	 * @return optional containing the account if found, empty otherwise
	 */
	Optional<Account> findByUniqueId(UUID uniqueId);

	/**
	 * Create a new account record.
	 *
	 * @param account the account to create
	 * @return the created account
	 */
	Account create(Account account);

	/**
	 * Update the last-seen timestamp for an account.
	 *
	 * @param uniqueId the Identica account unique id
	 * @param lastSeenAt epoch millis when the account was last seen
	 */
	void updateLastSeen(UUID uniqueId, long lastSeenAt);

	/**
	 * Delete an account record.
	 *
	 * @param uniqueId the Identica account unique id
	 */
	void delete(UUID uniqueId);
}
