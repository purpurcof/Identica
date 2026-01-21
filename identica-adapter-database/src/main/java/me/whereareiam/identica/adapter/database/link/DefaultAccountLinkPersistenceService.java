package me.whereareiam.identica.adapter.database.link;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.adapter.database.entity.AccountLinkEntity;
import me.whereareiam.identica.adapter.database.mapper.AccountLinkMapper;
import me.whereareiam.identica.adapter.database.repository.link.AccountLinkRepository;
import me.whereareiam.identica.database.AccountLinkPersistenceService;
import me.whereareiam.identica.logging.Logger;
import me.whereareiam.identica.model.AccountLink;
import org.jdbi.v3.core.Jdbi;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class DefaultAccountLinkPersistenceService implements AccountLinkPersistenceService {
	private final AccountLinkRepository repository;
	private final Jdbi jdbi;

	@Override
	public Optional<AccountLink> findByUniqueIdAndProviderId(UUID uniqueId, String providerId) {
		if (uniqueId == null || providerId == null || providerId.isBlank()) {
			Logger.warn("Attempted to load account link with blank fields");
			return Optional.empty();
		}

		try {
			return jdbi.inTransaction(_ ->
					repository.findByUniqueIdAndProviderId(uniqueId, providerId)
							.map(AccountLinkMapper::toModel));
		} catch (Exception e) {
			Logger.warn("Failed to load account link %s/%s: %s", uniqueId, providerId, e.getMessage());
			return Optional.empty();
		}
	}

	@Override
	public List<AccountLink> findByUniqueId(UUID uniqueId) {
		if (uniqueId == null) {
			Logger.warn("Attempted to load account links with null identica unique id");
			return Collections.emptyList();
		}

		try {
			return jdbi.inTransaction(_ ->
					repository.findByUniqueId(uniqueId).stream()
							.map(AccountLinkMapper::toModel)
							.collect(Collectors.toList()));
		} catch (Exception e) {
			Logger.warn("Failed to load account links for %s: %s", uniqueId, e.getMessage());
			return Collections.emptyList();
		}
	}

	@Override
	public Optional<AccountLink> findPrimary(UUID uniqueId) {
		if (uniqueId == null) {
			Logger.warn("Attempted to load primary account link with null identica unique id");
			return Optional.empty();
		}

		try {
			return jdbi.inTransaction(_ ->
					repository.findPrimary(uniqueId).map(AccountLinkMapper::toModel));
		} catch (Exception e) {
			Logger.warn("Failed to load primary account link for %s: %s", uniqueId, e.getMessage());
			return Optional.empty();
		}
	}

	@Override
	public AccountLink touch(UUID uniqueId, String providerId, boolean preferPrimary, long timestamp) {
		if (uniqueId == null) throw new IllegalArgumentException("Account link identica unique id is required");
		if (providerId == null || providerId.isBlank()) throw new IllegalArgumentException("Account link provider id is required");

		try {
			return jdbi.inTransaction(_ -> {
				Optional<AccountLinkEntity> existing = repository.findByUniqueIdAndProviderId(
						uniqueId,
						providerId
				);
				boolean hasPrimary = repository.findPrimary(uniqueId).isPresent();
				boolean makePrimary = preferPrimary || !hasPrimary;

				if (existing.isPresent()) {
					AccountLinkEntity entity = existing.get();
					repository.updateLastUsed(uniqueId, providerId, timestamp);
					entity = entity.toBuilder()
							.lastUsedAt(timestamp)
							.build();

					if (makePrimary && !entity.isPrimary()) {
						repository.clearPrimary(uniqueId);
						repository.setPrimary(uniqueId, providerId);
						entity = entity.toBuilder()
								.primary(true)
								.build();
					}

					return AccountLinkMapper.toModel(entity);
				}

				if (makePrimary) repository.clearPrimary(uniqueId);

				AccountLinkEntity entity = AccountLinkEntity.builder()
						.uniqueId(uniqueId)
						.providerId(providerId)
						.primary(makePrimary)
						.linkedAt(timestamp)
						.lastUsedAt(timestamp)
						.build();
				repository.insert(
						entity.getUniqueId(),
						entity.getProviderId(),
						entity.isPrimary(),
						entity.getLinkedAt(),
						entity.getLastUsedAt()
				);

				return AccountLinkMapper.toModel(entity);
			});
		} catch (Exception e) {
			Logger.warn("Failed to touch account link %s/%s: %s", uniqueId, providerId, e.getMessage());
			throw e;
		}
	}

	@Override
	public void setPrimary(UUID uniqueId, String providerId) {
		if (uniqueId == null || providerId == null || providerId.isBlank()) {
			Logger.warn("Attempted to set primary account link with blank fields");
			return;
		}

		try {
			jdbi.useTransaction(_ -> {
				repository.clearPrimary(uniqueId);
				repository.setPrimary(uniqueId, providerId);
			});
		} catch (Exception e) {
			Logger.warn("Failed to set primary account link %s/%s: %s", uniqueId, providerId, e.getMessage());
		}
	}

	@Override
	public void delete(UUID uniqueId, String providerId) {
		if (uniqueId == null || providerId == null || providerId.isBlank()) {
			Logger.warn("Attempted to delete account link with blank fields");
			return;
		}

		try {
			jdbi.useTransaction(_ -> repository.delete(uniqueId, providerId));
		} catch (Exception e) {
			Logger.warn("Failed to delete account link %s/%s: %s", uniqueId, providerId, e.getMessage());
		}
	}
}
