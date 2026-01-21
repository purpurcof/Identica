package me.whereareiam.identica.provider.premium.database;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.whereareiam.identica.database.DatabaseService;
import me.whereareiam.identica.provider.premium.database.entity.PremiumIdentityEntity;
import me.whereareiam.identica.provider.premium.database.repository.PremiumIdentityRepository;
import org.jdbi.v3.core.Jdbi;

import java.util.Optional;
import java.util.UUID;

@Singleton
public class PremiumIdentityPersistenceService {
	private final DatabaseService databaseService;
	private final Jdbi jdbi;
	private final PremiumIdentityRepository repository;

	@Inject
	public PremiumIdentityPersistenceService(
			DatabaseService databaseService,
			Jdbi jdbi,
			PremiumIdentityRepository repository
	) {
		this.databaseService = databaseService;
		this.jdbi = jdbi;
		this.repository = repository;
	}

	public void upsert(UUID identicaUniqueId, UUID mojangUniqueId) {
		if (identicaUniqueId == null || mojangUniqueId == null) {
			return;
		}
		if (!databaseService.isInitialized()) {
			return;
		}

		try {
			jdbi.useTransaction(handle -> {
				PremiumIdentityRepository transactionRepository = handle.attach(PremiumIdentityRepository.class);
				transactionRepository.deleteByIdenticaOrMojang(identicaUniqueId, mojangUniqueId);
				transactionRepository.insert(identicaUniqueId, mojangUniqueId);
			});
		} catch (Exception e) {
			throw new RuntimeException("Failed to upsert premium identity", e);
		}
	}

	public Optional<UUID> findIdenticaUniqueId(UUID mojangUniqueId) {
		if (mojangUniqueId == null) {
			return Optional.empty();
		}
		return repository.findByMojangUniqueId(mojangUniqueId)
				.map(PremiumIdentityEntity::getIdenticaUniqueId);
	}
}
