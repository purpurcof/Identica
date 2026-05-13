package me.whereareiam.identica.provider.credential.database;

import me.whereareiam.identica.provider.credential.model.CredentialAccount;
import me.whereareiam.identica.provider.credential.model.CredentialAccountHistory;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public interface CredentialAccountPersistenceService {
	@NotNull Optional<CredentialAccount> findBySubject(@NotNull String providerId, @NotNull String providerSubject);

	@NotNull CredentialAccount create(@NotNull CredentialAccount credential);

	void updatePassword(
			@NotNull String providerId,
			@NotNull String providerSubject,
			@NotNull String passwordHash,
			@NotNull String hashingMethod,
			long updatedAt
	);

	void delete(@NotNull String providerId, @NotNull String providerSubject);

	void recordPasswordChange(@NotNull CredentialAccountHistory history);
}
