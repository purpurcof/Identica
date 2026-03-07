package me.whereareiam.identica.provider.cracked.pipeline.scenario.registration;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import me.whereareiam.identica.model.config.Settings;
import me.whereareiam.identica.model.pipeline.journey.stage.step.StepResult;
import me.whereareiam.identica.pipeline.ScenarioContext;
import me.whereareiam.identica.pipeline.state.PipelineStateStore;
import me.whereareiam.identica.provider.cracked.type.PasswordChangeReason;
import me.whereareiam.identica.provider.cracked.account.CrackedAccountService;
import me.whereareiam.identica.provider.cracked.cryptography.CryptographyService;
import me.whereareiam.identica.provider.cracked.cryptography.PasswordCandidate;
import me.whereareiam.identica.provider.cracked.config.CrackedMessages;
import me.whereareiam.identica.provider.cracked.config.CrackedSettings;
import me.whereareiam.identica.provider.cracked.util.PasswordRules;
import me.whereareiam.identica.provider.cracked.pipeline.CrackedRegistrationAttempt;
import me.whereareiam.identica.provider.cracked.pipeline.CrackedRegisterStateItem;
import org.jetbrains.annotations.NotNull;
import java.util.concurrent.CompletableFuture;

@Singleton
public class CrackedRegistrationStep extends AbstractCrackedRegistrationStep {
	private final Provider<CrackedSettings> settingsProvider;
	private final CrackedAccountService accountService;
	private final CryptographyService cryptographyService;
	private final PasswordRules passwordPolicy;

	@Inject
	public CrackedRegistrationStep(
			Provider<CrackedMessages> messagesProvider,
			Provider<CrackedSettings> settingsProvider,
			Provider<Settings> coreSettingsProvider,
			CrackedAccountService accountService,
			CryptographyService cryptographyService,
			PasswordRules passwordPolicy,
			PipelineStateStore pipelineStateStore
	) {
		super("cracked-registration", messagesProvider, coreSettingsProvider, pipelineStateStore);
		this.settingsProvider = settingsProvider;
		this.accountService = accountService;
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

		CrackedSettings settings = settingsProvider.get();
		CrackedMessages messages = messagesProvider.get();
		CrackedSettings.Scenario.Registration registrationSettings = settings != null
				&& settings.getScenario() != null
				? settings.getScenario().getRegistration()
				: null;

		if (registrationSettings == null || !registrationSettings.isEnabled()) {
			return CompletableFuture.completedFuture(StepResult.denied(messages.getScenario().getRegistration().getStatus().getDisabled()));
		}

		boolean requireRepeat = registrationSettings.isRequireRepeat();
		CrackedRegisterStateItem pending = getRegisterState(context);
		long ttlMs = registrationTtlMs();

		if (requireRepeat && pending != null)
			return CompletableFuture.completedFuture(StepResult.proceed(context));

		if (!requireRepeat && pending != null)
			clearRegisterState(context, ttlMs);

		CrackedRegistrationAttempt input = consumeRegistrationAttempt(context, ttlMs);

		if (input == null)
			return CompletableFuture.completedFuture(StepResult.waiting(joinRegisterPrompt(messages)));

		if (input.isConfirm())
			return CompletableFuture.completedFuture(StepResult.waiting(messages.getScenario().getRegistration().getStatus().getNoPending()));

		String error = passwordPolicy.validate(input.getPassword());
		if (error != null && !error.isBlank())
			return CompletableFuture.completedFuture(StepResult.waiting(error));

		PasswordCandidate candidate = cryptographyService.hash(input.getPassword());
		if (candidate == null)
			return CompletableFuture.completedFuture(StepResult.failed(""));

		if (requireRepeat) {
			CrackedRegisterStateItem stateItem = new CrackedRegisterStateItem(
					candidate.getPasswordHash(),
					candidate.getHashingMethod()
			);
			storeRegisterState(context, stateItem, ttlMs);
			return CompletableFuture.completedFuture(StepResult.proceed(context));
		}

		if (accountService.register(
				providerSubject,
				candidate.getPasswordHash(),
				candidate.getHashingMethod(),
				PasswordChangeReason.REGISTER
		).isEmpty()) {
			return CompletableFuture.completedFuture(StepResult.failed(""));
		}

		return CompletableFuture.completedFuture(StepResult.complete(context));
	}

	private String joinRegisterPrompt(CrackedMessages messages) {
		return joinLines(messages.getScenario().getRegistration().getPrompt());
	}

}
