package me.whereareiam.identica.database;

import me.whereareiam.identica.model.identity.provider.AccountProviderLink;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for persisting and retrieving provider links.
 */
@SuppressWarnings("unused")
public interface ProviderLinkPersistenceService {
	/**
	 * Find a provider link by provider id and subject.
	 *
	 * @param providerId provider id
	 * @param providerSubject provider subject
	 * @return link or empty if not found
	 */
	@NotNull Optional<AccountProviderLink> findBySubject(@NotNull String providerId, @NotNull String providerSubject);

	/**
	 * Find a provider link by identity id and provider id.
	 *
	 * @param uniqueId identity id
	 * @param providerId provider id
	 * @return link or empty if not found
	 */
	@NotNull Optional<AccountProviderLink> findByUniqueIdAndProviderId(@NotNull UUID uniqueId, @NotNull String providerId);

	/**
	 * Find all links for an identity.
	 *
	 * @param uniqueId identity id
	 * @return list of links
	 */
	@NotNull List<AccountProviderLink> findByUniqueId(@NotNull UUID uniqueId);

	/**
	 * Create or update a provider link.
	 *
	 * @param link link to upsert
	 * @return stored link
	 */
	@NotNull AccountProviderLink upsert(@NotNull AccountProviderLink link);

	/**
	 * Delete all links for an identity.
	 *
	 * @param uniqueId identity id
	 */
	void deleteAll(@NotNull UUID uniqueId);
}
