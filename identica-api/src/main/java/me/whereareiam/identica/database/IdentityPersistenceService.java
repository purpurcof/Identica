package me.whereareiam.identica.database;

import me.whereareiam.identica.model.Identity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for persisting and retrieving identity mappings.
 */
@SuppressWarnings("unused")
public interface IdentityPersistenceService {
	/**
	 * Find an identity by provider id and provider subject.
	 *
	 * @param providerId the provider identifier (e.g., "Premium", "Cracked")
	 * @param providerSubject the provider-specific subject (e.g., Mojang UUID or username)
	 * @return optional containing the identity if found, empty otherwise
	 */
	Optional<Identity> findByProvider(String providerId, String providerSubject);

	/**
	 * Find all identities linked to an account.
	 *
	 * @param uniqueId the Identica account unique id
	 * @return list of identities (empty if none)
	 */
	List<Identity> findByAccount(UUID uniqueId);

	/**
	 * Create a new identity record.
	 *
	 * @param identity the identity to create
	 * @return the created identity
	 */
	Identity create(Identity identity);

	/**
	 * Update the last-used timestamp for an identity.
	 *
	 * @param providerId the provider identifier
	 * @param providerSubject the provider-specific subject
	 * @param lastUsedAt epoch millis when the identity was last used
	 */
	void updateLastUsed(String providerId, String providerSubject, long lastUsedAt);

	/**
	 * Delete an identity record.
	 *
	 * @param providerId the provider identifier
	 * @param providerSubject the provider-specific subject
	 */
	void delete(String providerId, String providerSubject);
}
