package me.whereareiam.identica.adapter.database.identity;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.adapter.database.entity.IdentityEntity;
import me.whereareiam.identica.adapter.database.mapper.IdentityMapper;
import me.whereareiam.identica.adapter.database.repository.identity.IdentityRepository;
import me.whereareiam.identica.database.IdentityPersistenceService;
import me.whereareiam.identica.logging.Logger;
import me.whereareiam.identica.model.Identity;
import org.jdbi.v3.core.Jdbi;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class DefaultIdentityPersistenceService implements IdentityPersistenceService {
	private final IdentityRepository identityRepository;
	private final Jdbi jdbi;

	@Override
	public Optional<Identity> findByProvider(String providerId, String providerSubject) {
		if (providerId == null || providerId.isBlank() || providerSubject == null || providerSubject.isBlank()) {
			Logger.warn("Attempted to load identity with blank provider fields");
			return Optional.empty();
		}

		try {
			return jdbi.inTransaction(_ ->
					identityRepository.findByProvider(providerId, providerSubject).map(IdentityMapper::toModel));
		} catch (Exception e) {
			Logger.warn("Failed to load identity %s/%s: %s", providerId, providerSubject, e.getMessage());
			return Optional.empty();
		}
	}

	@Override
	public List<Identity> findByAccount(UUID uniqueId) {
		if (uniqueId == null) {
			Logger.warn("Attempted to load identities with null unique id");
			return Collections.emptyList();
		}

		try {
			return jdbi.inTransaction(_ ->
					identityRepository.findByAccount(uniqueId).stream().map(IdentityMapper::toModel).toList());
		} catch (Exception e) {
			Logger.warn("Failed to load identities for %s: %s", uniqueId, e.getMessage());
			return Collections.emptyList();
		}
	}

	@Override
	public Identity create(Identity identity) {
		if (identity == null || identity.getUniqueId() == null) {
			throw new IllegalArgumentException("Identity unique id is required");
		}
		if (identity.getProviderId() == null || identity.getProviderId().isBlank()
				|| identity.getProviderSubject() == null || identity.getProviderSubject().isBlank()) {
			throw new IllegalArgumentException("Identity provider fields are required");
		}

		IdentityEntity entity = IdentityMapper.toEntity(identity);
		try {
			jdbi.useTransaction(_ -> identityRepository.save(entity));
			return IdentityMapper.toModel(entity);
		} catch (Exception e) {
			Logger.warn("Failed to create identity %s/%s: %s",
					identity.getProviderId(), identity.getProviderSubject(), e.getMessage());
			throw e;
		}
	}

	@Override
	public void updateLastUsed(String providerId, String providerSubject, long lastUsedAt) {
		if (providerId == null || providerId.isBlank() || providerSubject == null || providerSubject.isBlank()) {
			Logger.warn("Attempted to update identity with blank provider fields");
			return;
		}

		try {
			jdbi.useTransaction(_ -> identityRepository.updateLastUsed(providerId, providerSubject, lastUsedAt));
		} catch (Exception e) {
			Logger.warn("Failed to update identity %s/%s: %s", providerId, providerSubject, e.getMessage());
		}
	}

	@Override
	public void delete(String providerId, String providerSubject) {
		if (providerId == null || providerId.isBlank() || providerSubject == null || providerSubject.isBlank()) {
			Logger.warn("Attempted to delete identity with blank provider fields");
			return;
		}

		try {
			jdbi.useTransaction(_ -> identityRepository.delete(providerId, providerSubject));
		} catch (Exception e) {
			Logger.warn("Failed to delete identity %s/%s: %s", providerId, providerSubject, e.getMessage());
		}
	}

}
