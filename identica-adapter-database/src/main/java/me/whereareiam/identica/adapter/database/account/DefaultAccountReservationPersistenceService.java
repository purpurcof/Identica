package me.whereareiam.identica.adapter.database.account;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import me.whereareiam.identica.adapter.database.entity.account.AccountReservationEntity;
import me.whereareiam.identica.adapter.database.repository.account.AccountReservationRepository;
import me.whereareiam.identica.database.AccountReservationPersistenceService;
import me.whereareiam.identica.logging.Logger;
import me.whereareiam.identica.model.config.persistence.Persistence;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;

@Singleton
public class DefaultAccountReservationPersistenceService implements AccountReservationPersistenceService {
	private final AccountReservationRepository repository;
	private final Provider<Persistence> persistenceProvider;

	@Inject
	public DefaultAccountReservationPersistenceService(
			AccountReservationRepository repository,
			Provider<Persistence> persistenceProvider
	) {
		this.repository = repository;
		this.persistenceProvider = persistenceProvider;
	}

	@Override
	public @NotNull Optional<UUID> find(@NotNull String reservationKey) {
		if (reservationKey.isBlank()) return Optional.empty();

		try {
			long now = System.currentTimeMillis();
			return repository.find(reservationKey)
					.filter(reservation -> !isExpired(reservation.getExpiresAt(), now))
					.map(AccountReservationEntity::getUniqueId);
		} catch (Exception e) {
			Logger.warn("Failed to load account reservation %s: %s", reservationKey, e.getMessage());
			return Optional.empty();
		}
	}

	@Override
	public void reserve(@NotNull String reservationKey, @NotNull UUID uniqueId, long createdAt, long expiresAt) {
		if (reservationKey.isBlank()) return;
		if (expiresAt <= createdAt) return;

		try {
			switch (persistenceProvider.get().getType()) {
				case H2 -> repository.upsertH2(reservationKey, uniqueId, createdAt, expiresAt);
				case POSTGRES, SQLITE -> repository.upsertPostgresSqlite(reservationKey, uniqueId, createdAt, expiresAt);
				case MYSQL -> repository.upsertMysql(reservationKey, uniqueId, createdAt, expiresAt);
			}
		} catch (Exception e) {
			Logger.warn("Failed to reserve account UUID %s for %s: %s", uniqueId, reservationKey, e.getMessage());
		}
	}

	@Override
	public void delete(@NotNull String reservationKey) {
		if (reservationKey.isBlank()) return;

		try {
			repository.delete(reservationKey);
		} catch (Exception e) {
			Logger.warn("Failed to delete account reservation %s: %s", reservationKey, e.getMessage());
		}
	}

	@Override
	public void deleteByUniqueId(@NotNull UUID uniqueId) {
		try {
			repository.deleteByUniqueId(uniqueId);
		} catch (Exception e) {
			Logger.warn("Failed to delete account reservations for %s: %s", uniqueId, e.getMessage());
		}
	}

	@Override
	public void deleteExpired(long now) {
		try {
			repository.deleteExpired(now);
		} catch (Exception e) {
			Logger.warn("Failed to delete expired account reservations: %s", e.getMessage());
		}
	}

	private boolean isExpired(long expiresAt, long now) {
		if (expiresAt > now) return false;
		repository.deleteExpired(now);
		return true;
	}
}
