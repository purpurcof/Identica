package me.whereareiam.identica.provider.cracked.account;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.event.EventListener;
import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.event.base.IdenticEvent;
import me.whereareiam.identica.provider.cracked.config.CrackedSettings;
import me.whereareiam.identica.provider.cracked.cryptography.CryptographyService;
import me.whereareiam.identica.provider.cracked.cryptography.PasswordCandidate;
import me.whereareiam.identica.provider.cracked.event.PasswordVerifiedEvent;
import me.whereareiam.identica.provider.cracked.type.PasswordChangeReason;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class AutoupgradeLifecycle implements EventListener {
	private final Provider<CrackedSettings> settingsProvider;
	private final CryptographyService cryptographyService;
	private final CrackedAccountService accountService;
	private final EventManager eventManager;

	@Inject
	void registerListeners() {
		eventManager.register(this);
	}

	@IdenticEvent
	public void onPasswordVerified(@NotNull PasswordVerifiedEvent event) {
		CrackedSettings.Cryptography cryptography = resolveCryptography();
		if (cryptography == null || !cryptography.isAutoupgrade())
			return;

		String configuredId = normalize(cryptography.getAlgorithm());
		String currentId = normalize(event.getAccount().getHashingMethod());
		if (configuredId == null || currentId == null || configuredId.equals(currentId))
			return;

		PasswordCandidate candidate = cryptographyService.hash(event.getPassword());
		if (candidate == null)
			return;

		accountService.updatePassword(
				event.getAccount(),
				candidate.getPasswordHash(),
				candidate.getHashingMethod(),
				PasswordChangeReason.REHASH
		);
	}

	private @Nullable CrackedSettings.Cryptography resolveCryptography() {
		CrackedSettings settings = settingsProvider.get();
		return settings != null ? settings.getCryptography() : null;
	}

	private @Nullable String normalize(@Nullable String value) {
		if (value == null || value.isBlank()) return null;
		return value.trim().toLowerCase(Locale.ROOT);
	}
}