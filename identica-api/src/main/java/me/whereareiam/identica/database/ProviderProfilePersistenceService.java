package me.whereareiam.identica.database;

import me.whereareiam.identica.model.identity.provider.AccountProviderProfile;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Service for persisting and retrieving provider profiles.
 */
public interface ProviderProfilePersistenceService {
	/**
	 * Find a provider profile by provider id and subject.
	 *
	 * @param providerId provider id
	 * @param providerSubject provider subject
	 * @return profile or empty if not found
	 */
	@NotNull Optional<AccountProviderProfile> findBySubject(@NotNull String providerId, @NotNull String providerSubject);

	/**
	 * Create or update a provider profile.
	 *
	 * @param profile profile to upsert
	 * @return stored profile
	 */
	@NotNull AccountProviderProfile upsert(@NotNull AccountProviderProfile profile);

	/**
	 * Delete a provider profile by provider id and subject.
	 *
	 * @param providerId provider id
	 * @param providerSubject provider subject
	 */
	void delete(@NotNull String providerId, @NotNull String providerSubject);
}
