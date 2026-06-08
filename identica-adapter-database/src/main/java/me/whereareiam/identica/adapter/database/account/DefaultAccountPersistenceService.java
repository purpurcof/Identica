package me.whereareiam.identica.adapter.database.account;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.adapter.database.entity.account.AccountEntity;
import me.whereareiam.identica.adapter.database.mapper.account.AccountMapper;
import me.whereareiam.identica.adapter.database.repository.account.AccountRepository;
import me.whereareiam.identica.database.AccountPersistenceService;
import me.whereareiam.identica.event.EventListener;
import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.event.account.AccountLifecycleEvent;
import me.whereareiam.identica.event.base.IdenticEvent;
import me.whereareiam.identica.logging.Logger;
import me.whereareiam.identica.model.identity.Account;
import me.whereareiam.identica.type.event.EventOrder;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class DefaultAccountPersistenceService implements AccountPersistenceService, EventListener {
	private final AccountRepository accountRepository;
	private final EventManager eventManager;

	@Inject
	void registerListeners() {
		eventManager.register(this);
	}

	@Override
	public @NotNull Optional<Account> findByUniqueId(@NotNull UUID uniqueId) {
		try {
			return accountRepository.findByUniqueId(uniqueId).map(AccountMapper::toModel);
		} catch (Exception e) {
			Logger.warn("Failed to load account %s: %s", uniqueId, e.getMessage());
			return Optional.empty();
		}
	}

	@Override
	public @NotNull List<Account> findByUsername(@NotNull String username) {
		if (username.isBlank()) return List.of();

		try {
			return accountRepository.findByUsername(username).stream()
					.map(AccountMapper::toModel)
					.toList();
		} catch (Exception e) {
			Logger.warn("Failed to load accounts by username %s: %s", username, e.getMessage());
			return List.of();
		}
	}

	@Override
	public long count() {
		try {
			return accountRepository.count();
		} catch (Exception e) {
			Logger.warn("Failed to count accounts: %s", e.getMessage());
			return 0L;
		}
	}

	@Override
	public @NotNull Account create(@NotNull Account account) {
		AccountEntity entity = AccountMapper.toEntity(account);

		try {
			accountRepository.insert(
					entity.getUniqueId(),
					entity.getUsername(),
					entity.getCreatedAt(),
					entity.getLastSeenAt()
			);

			return AccountMapper.toModel(entity);
		} catch (Exception e) {
			Logger.warn("Failed to create account %s: %s", account.getUniqueId(), e.getMessage());
			throw e;
		}
	}

	@Override
	public void updateLastSeen(@NotNull UUID uniqueId, long lastSeenAt) {
		try {
			accountRepository.updateLastSeen(uniqueId, lastSeenAt);
		} catch (Exception e) {
			Logger.warn("Failed to update last seen for %s: %s", uniqueId, e.getMessage());
		}
	}

	@Override
	public void updateUsername(@NotNull UUID uniqueId, @NotNull String username) {
		if (username.isBlank()) return;

		try {
			accountRepository.updateUsername(uniqueId, username);
		} catch (Exception e) {
			Logger.warn("Failed to update username for %s: %s", uniqueId, e.getMessage());
		}
	}

	@Override
	public void delete(@NotNull UUID uniqueId) {
		try {
			accountRepository.delete(uniqueId);
		} catch (Exception e) {
			Logger.warn("Failed to delete account %s: %s", uniqueId, e.getMessage());
		}
	}

	@IdenticEvent(EventOrder.HIGHEST)
	public void onAccountLifecycle(@NotNull AccountLifecycleEvent event) {
		UUID uniqueId = event.getIdentity().getAccountUniqueId();
		if (uniqueId == null) return;

		delete(uniqueId);
	}
}
