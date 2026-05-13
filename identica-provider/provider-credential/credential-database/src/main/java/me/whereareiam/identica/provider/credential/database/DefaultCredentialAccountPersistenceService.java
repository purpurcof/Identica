package me.whereareiam.identica.provider.credential.database;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.provider.credential.database.mapper.CredentialAccountHistoryMapper;
import me.whereareiam.identica.provider.credential.database.mapper.CredentialAccountMapper;
import me.whereareiam.identica.provider.credential.database.repository.CredentialAccountHistoryRepository;
import me.whereareiam.identica.provider.credential.database.repository.CredentialAccountRepository;
import me.whereareiam.identica.provider.credential.model.CredentialAccount;
import me.whereareiam.identica.provider.credential.model.CredentialAccountHistory;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class DefaultCredentialAccountPersistenceService implements CredentialAccountPersistenceService {
	private final CredentialAccountRepository credentialRepository;
	private final CredentialAccountHistoryRepository historyRepository;

	@Override
	public @NotNull Optional<CredentialAccount> findBySubject(
			@NotNull String providerId,
			@NotNull String providerSubject
	) {
		if (providerId.isBlank() || providerSubject.isBlank()) return Optional.empty();
		return credentialRepository.findBySubject(providerId, providerSubject)
				.map(CredentialAccountMapper::toModel);
	}

	@Override
	public @NotNull CredentialAccount create(@NotNull CredentialAccount credential) {
		CredentialAccount existing = findBySubject(credential.getProviderId(), credential.getProviderSubject()).orElse(null);
		if (existing != null) return existing;

		credentialRepository.insert(
				credential.getProviderId(),
				credential.getProviderSubject(),
				credential.getPasswordHash(),
				credential.getHashingMethod(),
				credential.getCreatedAt(),
				credential.getUpdatedAt()
		);
		return credential;
	}

	@Override
	public void updatePassword(
			@NotNull String providerId,
			@NotNull String providerSubject,
			@NotNull String passwordHash,
			@NotNull String hashingMethod,
			long updatedAt
	) {
		credentialRepository.updatePassword(providerId, providerSubject, passwordHash, hashingMethod, updatedAt);
	}

	@Override
	public void delete(@NotNull String providerId, @NotNull String providerSubject) {
		credentialRepository.delete(providerId, providerSubject);
	}

	@Override
	public void recordPasswordChange(@NotNull CredentialAccountHistory history) {
		var entity = CredentialAccountHistoryMapper.toEntity(history);
		if (entity == null)
			return;
		historyRepository.insert(
				entity.getProviderId(),
				entity.getProviderSubject(),
				entity.getHashingMethod(),
				entity.getChangeReason(),
				entity.getChangedAt()
		);
	}
}
