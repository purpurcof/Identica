package me.whereareiam.identica.database;

import me.whereareiam.identica.model.AccountLink;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for persisting and retrieving account-provider links.
 */
@SuppressWarnings("unused")
public interface AccountLinkPersistenceService {
	/**
	 * Find a link by account id and provider id.
	 *
	 * @param uniqueId the Identica account unique id
	 * @param providerId the provider id
	 * @return optional containing the link if found, empty otherwise
	 */
	Optional<AccountLink> findByUniqueIdAndProviderId(UUID uniqueId, String providerId);

	/**
	 * Find all links for a given account.
	 *
	 * @param uniqueId the Identica account unique id
	 * @return list of links
	 */
	List<AccountLink> findByUniqueId(UUID uniqueId);

	/**
	 * Find the primary link for a given account.
	 *
	 * @param uniqueId the Identica account unique id
	 * @return optional containing the primary link if found, empty otherwise
	 */
	Optional<AccountLink> findPrimary(UUID uniqueId);

	/**
	 * Create or update a link, updating its last-used timestamp.
	 *
	 * @param uniqueId the Identica account unique id
	 * @param providerId the provider id
	 * @param preferPrimary whether to make this provider primary if possible
	 * @param timestamp epoch millis for linked/last-used timestamps
	 * @return the resulting link
	 */
	AccountLink touch(UUID uniqueId, String providerId, boolean preferPrimary, long timestamp);

	/**
	 * Set a link as the primary provider for the account.
	 *
	 * @param uniqueId the Identica account unique id
	 * @param providerId the provider id
	 */
	void setPrimary(UUID uniqueId, String providerId);

	/**
	 * Delete a link.
	 *
	 * @param uniqueId the Identica account unique id
	 * @param providerId the provider id
	 */
	void delete(UUID uniqueId, String providerId);
}
