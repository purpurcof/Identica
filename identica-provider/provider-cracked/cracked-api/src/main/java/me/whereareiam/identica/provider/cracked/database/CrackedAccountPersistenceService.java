package me.whereareiam.identica.provider.cracked.database;

import me.whereareiam.identica.provider.cracked.model.CrackedAccount;
import me.whereareiam.identica.provider.cracked.model.CrackedAccountPassword;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public interface CrackedAccountPersistenceService {
	@NotNull Optional<CrackedAccount> findBySubject(@NotNull String providerId, @NotNull String providerSubject);

	@NotNull CrackedAccount create(@NotNull CrackedAccount account);

	void updatePassword(
			@NotNull String providerId,
			@NotNull String providerSubject,
			@NotNull String passwordHash,
			@NotNull String hashingMethod,
			long updatedAt
	);

	void delete(@NotNull String providerId, @NotNull String providerSubject);

	void recordPasswordChange(@NotNull CrackedAccountPassword change);
}
