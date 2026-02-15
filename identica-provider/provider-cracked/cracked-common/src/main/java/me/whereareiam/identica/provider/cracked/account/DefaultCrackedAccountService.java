package me.whereareiam.identica.provider.cracked.account;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.provider.cracked.CrackedConstants;
import me.whereareiam.identica.provider.cracked.database.CrackedAccountPersistenceService;
import me.whereareiam.identica.provider.cracked.event.AccountRegisteredEvent;
import me.whereareiam.identica.provider.cracked.event.PasswordChangedEvent;
import me.whereareiam.identica.provider.cracked.model.CrackedAccount;
import me.whereareiam.identica.provider.cracked.model.CrackedAccountPassword;
import me.whereareiam.identica.provider.cracked.type.PasswordChangeReason;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class DefaultCrackedAccountService implements CrackedAccountService {
	private final CrackedAccountPersistenceService persistenceService;
	private final EventManager eventManager;

	@Override
	public @NotNull Optional<CrackedAccount> find(@Nullable String providerSubject) {
		if (providerSubject == null || providerSubject.isBlank())
			return Optional.empty();
		return persistenceService.findBySubject(CrackedConstants.PROVIDER_ID, providerSubject);
	}

	@Override
	public @NotNull Optional<CrackedAccount> register(
			@NotNull String providerSubject,
			@NotNull String passwordHash,
			@NotNull String hashingMethod,
			@NotNull PasswordChangeReason reason
	) {
		if (providerSubject.isBlank())
			return Optional.empty();

		CrackedAccount existing = find(providerSubject).orElse(null);
		if (existing != null)
			return Optional.empty();

		long now = System.currentTimeMillis();
		CrackedAccount account = CrackedAccount.builder()
				.providerId(CrackedConstants.PROVIDER_ID)
				.providerSubject(providerSubject)
				.passwordHash(passwordHash)
				.hashingMethod(hashingMethod)
				.createdAt(now)
				.updatedAt(now)
				.build();

		persistenceService.create(account);
		recordChange(providerSubject, hashingMethod, reason, now);
		eventManager.call(new AccountRegisteredEvent(account, reason));
		return Optional.of(account);
	}

	@Override
	public boolean updatePassword(
			@NotNull CrackedAccount account,
			@NotNull String passwordHash,
			@NotNull String hashingMethod,
			@NotNull PasswordChangeReason reason
	) {
		long now = System.currentTimeMillis();
		persistenceService.updatePassword(
				account.getProviderId(),
				account.getProviderSubject(),
				passwordHash,
				hashingMethod,
				now
		);

		recordChange(account.getProviderSubject(), hashingMethod, reason, now);
		account.setPasswordHash(passwordHash);
		account.setHashingMethod(hashingMethod);
		account.setUpdatedAt(now);
		eventManager.call(new PasswordChangedEvent(account, reason, now));
		return true;
	}

	@Override
	public void delete(@NotNull String providerSubject) {
		if (providerSubject.isBlank()) return;
		persistenceService.delete(CrackedConstants.PROVIDER_ID, providerSubject);
	}

	private void recordChange(
			@NotNull String providerSubject,
			@NotNull String hashingMethod,
			@NotNull PasswordChangeReason reason,
			long now
	) {
		persistenceService.recordPasswordChange(CrackedAccountPassword.builder()
				.providerId(CrackedConstants.PROVIDER_ID)
				.providerSubject(providerSubject)
				.hashingMethod(hashingMethod)
				.changeReason(reason)
				.changedAt(now)
				.build());
	}
}
