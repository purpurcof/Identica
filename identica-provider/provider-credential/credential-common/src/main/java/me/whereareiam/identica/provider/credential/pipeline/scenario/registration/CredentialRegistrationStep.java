package me.whereareiam.identica.provider.credential.pipeline.scenario.registration;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import me.whereareiam.identica.model.config.Settings;
import me.whereareiam.identica.model.pipeline.journey.stage.step.StepResult;
import me.whereareiam.identica.pipeline.ScenarioContext;
import me.whereareiam.identica.pipeline.state.PipelineStateStore;
import me.whereareiam.identica.provider.credential.account.CredentialAccountService;
import me.whereareiam.identica.provider.credential.config.CredentialMessages;
import me.whereareiam.identica.provider.credential.config.CredentialSettings;
import me.whereareiam.identica.provider.credential.cryptography.CryptographyService;
import me.whereareiam.identica.provider.credential.cryptography.PasswordCandidate;
import me.whereareiam.identica.provider.credential.pipeline.CredentialRegisterStateItem;
import me.whereareiam.identica.provider.credential.pipeline.CredentialRegistrationAttempt;
import me.whereareiam.identica.provider.credential.type.PasswordChangeReason;
import me.whereareiam.identica.provider.credential.util.PasswordRules;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

@Singleton
public class CredentialRegistrationStep extends AbstractCredentialRegistrationStep {
	private final Provider<CredentialSettings> settingsProvider;
	private final CredentialAccountService credentialService;
	private final CryptographyService cryptographyService;
	private final PasswordRules passwordPolicy;

	@Inject
	public CredentialRegistrationStep(
			Provider<CredentialMessages> messagesProvider,
			Provider<CredentialSettings> settingsProvider,
			Provider<Settings> coreSettingsProvider,
			CredentialAccountService credentialService,
			CryptographyService cryptographyService,
			PasswordRules passwordPolicy,
			PipelineStateStore pipelineStateStore
	) {
		super("password-registration", messagesProvider, coreSettingsProvider, pipelineStateStore);
		this.settingsProvider = settingsProvider;
		this.credentialService = credentialService;
		this.cryptographyService = cryptographyService;
		this.passwordPolicy = passwordPolicy;
	}

	@Override
	public int order() {
		return 10;
	}

	@Override
	public @NotNull CompletableFuture<StepResult> execute(@NotNull ScenarioContext context) {
		String providerSubject = requireProviderSubject(context);

		CredentialSettings settings = settingsProvider.get();
		CredentialMessages messages = messagesProvider.get();
		CredentialSettings.Scenario.Registration registrationSettings = settings != null
				&& settings.getScenario() != null
				? settings.getScenario().getRegistration()
				: null;

		if (registrationSettings == null || !registrationSettings.isEnabled()) {
			return CompletableFuture.completedFuture(StepResult.denied(messages.getScenario().getRegistration().getStatus().getDisabled()));
		}

		boolean requireRepeat = registrationSettings.isRequireRepeat();
		CredentialRegisterStateItem pending = getRegisterState(context);
		long ttlMs = registrationTtlMs();

		if (requireRepeat && pending != null) return CompletableFuture.completedFuture(StepResult.proceed(context));

		if (!requireRepeat && pending != null) clearRegisterState(context, ttlMs);

		CredentialRegistrationAttempt input = consumeRegistrationAttempt(context, ttlMs);

		if (input == null) return CompletableFuture.completedFuture(StepResult.waiting(joinRegisterPrompt(messages)));
		if (input.isConfirm()) return CompletableFuture.completedFuture(StepResult.waiting(messages.getScenario().getRegistration().getStatus().getNoPending()));

		String error = passwordPolicy.validate(input.getPassword());
		if (error != null && !error.isBlank()) return CompletableFuture.completedFuture(StepResult.waiting(error));

		PasswordCandidate candidate = cryptographyService.hash(input.getPassword());
		if (candidate == null) return CompletableFuture.completedFuture(StepResult.failed(""));

		if (requireRepeat) {
			CredentialRegisterStateItem stateItem = new CredentialRegisterStateItem(
					candidate.getPasswordHash(),
					candidate.getHashingMethod()
			);
			storeRegisterState(context, stateItem, ttlMs);
			return CompletableFuture.completedFuture(StepResult.proceed(context));
		}

		if (credentialService.register(
				providerSubject,
				candidate.getPasswordHash(),
				candidate.getHashingMethod(),
				PasswordChangeReason.REGISTER
		).isEmpty()) {
			return CompletableFuture.completedFuture(StepResult.failed(""));
		}

		return CompletableFuture.completedFuture(StepResult.complete(context));
	}

	private String joinRegisterPrompt(CredentialMessages messages) {
		return joinLines(messages.getScenario().getRegistration().getPrompt());
	}

}
