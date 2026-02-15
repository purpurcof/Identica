package me.whereareiam.identica.provider.cracked.database;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.provider.cracked.model.CrackedAccount;
import me.whereareiam.identica.provider.cracked.model.CrackedAccountPassword;
import me.whereareiam.identica.provider.cracked.database.mapper.CrackedAccountMapper;
import me.whereareiam.identica.provider.cracked.database.mapper.CrackedAccountPasswordMapper;
import me.whereareiam.identica.provider.cracked.database.repository.CrackedAccountPasswordRepository;
import me.whereareiam.identica.provider.cracked.database.repository.CrackedAccountRepository;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class DefaultCrackedAccountPersistenceService implements CrackedAccountPersistenceService {
	private final CrackedAccountRepository accountRepository;
	private final CrackedAccountPasswordRepository passwordRepository;

	@Override
	public @NotNull Optional<CrackedAccount> findBySubject(
			@NotNull String providerId,
			@NotNull String providerSubject
	) {
		if (providerId.isBlank() || providerSubject.isBlank())
			return Optional.empty();
		return accountRepository.findBySubject(providerId, providerSubject)
				.map(CrackedAccountMapper::toModel);
	}

	@Override
	public @NotNull CrackedAccount create(@NotNull CrackedAccount account) {
		CrackedAccount existing = findBySubject(account.getProviderId(), account.getProviderSubject())
				.orElse(null);
		if (existing != null)
			return existing;

		accountRepository.insert(
				account.getProviderId(),
				account.getProviderSubject(),
				account.getPasswordHash(),
				account.getHashingMethod(),
				account.getCreatedAt(),
				account.getUpdatedAt()
		);
		return account;
	}

	@Override
	public void updatePassword(
			@NotNull String providerId,
			@NotNull String providerSubject,
			@NotNull String passwordHash,
			@NotNull String hashingMethod,
			long updatedAt
	) {
		accountRepository.updatePassword(providerId, providerSubject, passwordHash, hashingMethod, updatedAt);
	}

	@Override
	public void delete(@NotNull String providerId, @NotNull String providerSubject) {
		accountRepository.delete(providerId, providerSubject);
	}

	@Override
	public void recordPasswordChange(@NotNull CrackedAccountPassword change) {
		var entity = CrackedAccountPasswordMapper.toEntity(change);
		if (entity == null)
			return;
		passwordRepository.insert(
				entity.getProviderId(),
				entity.getProviderSubject(),
				entity.getHashingMethod(),
				entity.getChangeReason(),
				entity.getChangedAt()
		);
	}
}
