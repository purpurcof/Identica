package me.whereareiam.identica.database.provider;

import me.whereareiam.identica.model.identity.provider.AccountProviderProfile;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Service for persisting and retrieving provider profiles.
 */
public interface ProviderProfilePersistenceService {
	/**
	 * Find a provider resolver by provider id and subject.
	 *
	 * @param providerId provider id
	 * @param providerSubject provider subject
	 * @return resolver or empty if not found
	 */
	@NotNull Optional<AccountProviderProfile> findBySubject(@NotNull String providerId, @NotNull String providerSubject);

	/**
	 * Create or update a provider resolver.
	 *
	 * @param profile resolver to upsert
	 * @return stored resolver
	 */
	@NotNull AccountProviderProfile upsert(@NotNull AccountProviderProfile profile);

	/**
	 * Delete a provider resolver by provider id and subject.
	 *
	 * @param providerId provider id
	 * @param providerSubject provider subject
	 */
	void delete(@NotNull String providerId, @NotNull String providerSubject);
}
