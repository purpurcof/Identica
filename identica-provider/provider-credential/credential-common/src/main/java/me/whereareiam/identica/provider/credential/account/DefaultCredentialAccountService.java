package me.whereareiam.identica.provider.credential.account;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.provider.credential.CredentialConstants;
import me.whereareiam.identica.provider.credential.database.CredentialAccountPersistenceService;
import me.whereareiam.identica.provider.credential.event.account.AccountRegisteredEvent;
import me.whereareiam.identica.provider.credential.event.account.password.PasswordChangedEvent;
import me.whereareiam.identica.provider.credential.model.CredentialAccount;
import me.whereareiam.identica.provider.credential.model.CredentialAccountHistory;
import me.whereareiam.identica.provider.credential.type.PasswordChangeReason;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class DefaultCredentialAccountService implements CredentialAccountService {
	private final CredentialAccountPersistenceService persistenceService;
	private final EventManager eventManager;

	@Override
	public @NotNull Optional<CredentialAccount> find(@Nullable String providerSubject) {
		if (providerSubject == null || providerSubject.isBlank()) return Optional.empty();
		return persistenceService.findBySubject(CredentialConstants.PROVIDER_ID, providerSubject);
	}

	@Override
	public @NotNull Optional<CredentialAccount> register(
			@NotNull String providerSubject,
			@NotNull String passwordHash,
			@NotNull String hashingMethod,
			@NotNull PasswordChangeReason reason
	) {
		if (providerSubject.isBlank()) return Optional.empty();

		CredentialAccount existing = find(providerSubject).orElse(null);
		if (existing != null) return Optional.empty();

		long now = System.currentTimeMillis();
		CredentialAccount credential = CredentialAccount.builder()
				.providerId(CredentialConstants.PROVIDER_ID)
				.providerSubject(providerSubject)
				.passwordHash(passwordHash)
				.hashingMethod(hashingMethod)
				.createdAt(now)
				.updatedAt(now)
				.build();

		persistenceService.create(credential);
		recordChange(providerSubject, hashingMethod, reason, now);
		eventManager.call(new AccountRegisteredEvent(credential, reason));
		return Optional.of(credential);
	}

	@Override
	public boolean updatePassword(
			@NotNull CredentialAccount credential,
			@NotNull String passwordHash,
			@NotNull String hashingMethod,
			@NotNull PasswordChangeReason reason
	) {
		long now = System.currentTimeMillis();
		persistenceService.updatePassword(
				credential.getProviderId(),
				credential.getProviderSubject(),
				passwordHash,
				hashingMethod,
				now
		);

		recordChange(credential.getProviderSubject(), hashingMethod, reason, now);
		credential.setPasswordHash(passwordHash);
		credential.setHashingMethod(hashingMethod);
		credential.setUpdatedAt(now);
		eventManager.call(new PasswordChangedEvent(credential, reason, now));
		return true;
	}

	@Override
	public void delete(@NotNull String providerSubject) {
		if (providerSubject.isBlank()) return;
		persistenceService.delete(CredentialConstants.PROVIDER_ID, providerSubject);
	}

	private void recordChange(
			@NotNull String providerSubject,
			@NotNull String hashingMethod,
			@NotNull PasswordChangeReason reason,
			long now
	) {
		persistenceService.recordPasswordChange(CredentialAccountHistory.builder()
				.providerId(CredentialConstants.PROVIDER_ID)
				.providerSubject(providerSubject)
				.hashingMethod(hashingMethod)
				.changeReason(reason)
				.changedAt(now)
				.build());
	}
}
