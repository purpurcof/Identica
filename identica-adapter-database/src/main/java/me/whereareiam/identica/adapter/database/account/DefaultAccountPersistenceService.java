package me.whereareiam.identica.adapter.database.account;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.adapter.database.entity.AccountEntity;
import me.whereareiam.identica.adapter.database.mapper.AccountMapper;
import me.whereareiam.identica.adapter.database.repository.account.AccountRepository;
import me.whereareiam.identica.database.AccountPersistenceService;
import me.whereareiam.identica.logging.Logger;
import me.whereareiam.identica.model.Account;
import org.jdbi.v3.core.Jdbi;

import java.util.Optional;
import java.util.UUID;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class DefaultAccountPersistenceService implements AccountPersistenceService {
	private final AccountRepository accountRepository;
	private final Jdbi jdbi;

	@Override
	public Optional<Account> findByUniqueId(UUID uniqueId) {
		if (uniqueId == null) {
			Logger.warn("Attempted to load account with null unique id");
			return Optional.empty();
		}

		try {
			return jdbi.inTransaction(_ ->
					accountRepository.findByUniqueId(uniqueId).map(AccountMapper::toModel));
		} catch (Exception e) {
			Logger.warn("Failed to load account %s: %s", uniqueId, e.getMessage());
			return Optional.empty();
		}
	}

	@Override
	public Account create(Account account) {
		if (account == null || account.getUniqueId() == null) {
			throw new IllegalArgumentException("Account unique id is required");
		}

		AccountEntity entity = AccountMapper.toEntity(account);
		try {
			jdbi.useTransaction(_ -> accountRepository.save(entity));
			return AccountMapper.toModel(entity);
		} catch (Exception e) {
			Logger.warn("Failed to create account %s: %s", account.getUniqueId(), e.getMessage());
			throw e;
		}
	}

	@Override
	public void updateLastSeen(UUID uniqueId, long lastSeenAt) {
		if (uniqueId == null) {
			Logger.warn("Attempted to update last seen for null unique id");
			return;
		}

		try {
			jdbi.useTransaction(_ -> accountRepository.updateLastSeen(uniqueId, lastSeenAt));
		} catch (Exception e) {
			Logger.warn("Failed to update last seen for %s: %s", uniqueId, e.getMessage());
		}
	}

	@Override
	public void delete(UUID uniqueId) {
		if (uniqueId == null) {
			Logger.warn("Attempted to delete account with null unique id");
			return;
		}

		try {
			jdbi.useTransaction(_ -> accountRepository.delete(uniqueId));
		} catch (Exception e) {
			Logger.warn("Failed to delete account %s: %s", uniqueId, e.getMessage());
		}
	}

}
