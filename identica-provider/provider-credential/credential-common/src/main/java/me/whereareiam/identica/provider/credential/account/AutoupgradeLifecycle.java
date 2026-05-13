package me.whereareiam.identica.provider.credential.account;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import me.whereareiam.identica.event.EventListener;
import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.event.base.IdenticEvent;
import me.whereareiam.identica.provider.credential.config.CredentialSettings;
import me.whereareiam.identica.provider.credential.cryptography.CryptographyService;
import me.whereareiam.identica.provider.credential.cryptography.PasswordCandidate;
import me.whereareiam.identica.provider.credential.event.account.password.PasswordVerifiedEvent;
import me.whereareiam.identica.provider.credential.type.PasswordChangeReason;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

@Singleton
public class AutoupgradeLifecycle implements EventListener {
	private final Provider<CredentialSettings> settingsProvider;
	private final CryptographyService cryptographyService;
	private final CredentialAccountService credentialService;

	@Inject
	public AutoupgradeLifecycle(
			Provider<CredentialSettings> settingsProvider,
			CryptographyService cryptographyService,
			CredentialAccountService credentialService,
			EventManager eventManager
	) {
		this.settingsProvider = settingsProvider;
		this.cryptographyService = cryptographyService;
		this.credentialService = credentialService;
		eventManager.register(this);
	}

	@IdenticEvent
	public void onPasswordVerified(@NotNull PasswordVerifiedEvent event) {
		CredentialSettings.Cryptography cryptography = resolveCryptography();
		if (cryptography == null || !cryptography.isAutoupgrade()) return;

		String configuredId = normalize(cryptography.getAlgorithm());
		String currentId = normalize(event.getCredential().getHashingMethod());
		if (configuredId == null || currentId == null || configuredId.equals(currentId))
			return;

		PasswordCandidate candidate = cryptographyService.hash(event.getPassword());
		if (candidate == null) return;

		credentialService.updatePassword(
				event.getCredential(),
				candidate.getPasswordHash(),
				candidate.getHashingMethod(),
				PasswordChangeReason.REHASH
		);
	}

	private @Nullable CredentialSettings.Cryptography resolveCryptography() {
		CredentialSettings settings = settingsProvider.get();
		return settings != null
				? settings.getCryptography()
				: null;
	}

	private @Nullable String normalize(@Nullable String value) {
		if (value == null || value.isBlank()) return null;
		return value.trim().toLowerCase(Locale.ROOT);
	}
}
