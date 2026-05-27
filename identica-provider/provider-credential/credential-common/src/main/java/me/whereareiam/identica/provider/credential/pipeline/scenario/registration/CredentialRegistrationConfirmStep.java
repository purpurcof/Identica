package me.whereareiam.identica.provider.credential.pipeline.scenario.registration;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import me.whereareiam.identica.model.config.Settings;
import me.whereareiam.identica.pipeline.state.PipelineStateStore;
import me.whereareiam.identica.provider.credential.account.CredentialAccountService;
import me.whereareiam.identica.provider.credential.config.CredentialMessages;
import me.whereareiam.identica.provider.credential.cryptography.CryptographyService;
import me.whereareiam.identica.provider.credential.pipeline.scenario.base.AbstractCredentialConfirmStep;
import org.jetbrains.annotations.NotNull;

@Singleton
public class CredentialRegistrationConfirmStep extends AbstractCredentialConfirmStep {
	@Inject
	public CredentialRegistrationConfirmStep(
			Provider<CredentialMessages> messagesProvider,
			Provider<Settings> coreSettingsProvider,
			CredentialAccountService credentialService,
			PipelineStateStore pipelineStateStore,
			CryptographyService cryptographyService
	) {
		super(
				"password-registration-confirm",
				messagesProvider,
				coreSettingsProvider,
				credentialService,
				pipelineStateStore,
				cryptographyService
		);
	}

	@Override
	public int order() {
		return 20;
	}

	@Override
	protected long ttlMs() {
		return registrationTtlMs();
	}

	@Override
	protected @NotNull String registerPrompt(@NotNull CredentialMessages messages) {
		return joinLines(messages.getScenario().getRegistration().getPrompt());
	}

	@Override
	protected @NotNull String confirmPrompt(@NotNull CredentialMessages messages) {
		return joinLines(messages.getScenario().getRegistration().getConfirmPrompt());
	}

	@Override
	protected @NotNull String mismatchMessage(@NotNull CredentialMessages messages) {
		return messages.getScenario().getRegistration().getStatus().getMismatch();
	}
}
