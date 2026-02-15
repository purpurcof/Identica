package me.whereareiam.identica.provider.cracked.pipeline.scenario.registration;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import me.whereareiam.identica.model.config.Settings;
import me.whereareiam.identica.model.pipeline.journey.stage.step.StepResult;
import me.whereareiam.identica.pipeline.ScenarioContext;
import me.whereareiam.identica.pipeline.state.PipelineStateStore;
import me.whereareiam.identica.provider.cracked.account.CrackedAccountService;
import me.whereareiam.identica.provider.cracked.cryptography.CryptographyService;
import me.whereareiam.identica.provider.cracked.config.CrackedMessages;
import me.whereareiam.identica.provider.cracked.model.CrackedAccount;
import me.whereareiam.identica.provider.cracked.pipeline.CrackedRegistrationAttempt;
import me.whereareiam.identica.provider.cracked.pipeline.CrackedRegisterStateItem;
import me.whereareiam.identica.provider.cracked.type.PasswordChangeReason;
import org.jetbrains.annotations.NotNull;
import java.util.concurrent.CompletableFuture;

@Singleton
public class RegistrationConfirmStep extends AbstractCrackedRegistrationStep {
	private final CrackedAccountService accountService;
	private final CryptographyService cryptographyService;

	@Inject
	public RegistrationConfirmStep(
			Provider<CrackedMessages> messagesProvider,
			Provider<Settings> coreSettingsProvider,
			CrackedAccountService accountService,
			PipelineStateStore pipelineStateStore,
			CryptographyService cryptographyService
	) {
		super("cracked-registration-confirm", messagesProvider, coreSettingsProvider, pipelineStateStore);
		this.accountService = accountService;
		this.cryptographyService = cryptographyService;
	}

	@Override
	public int order() {
		return 20;
	}

	@Override
	public @NotNull CompletableFuture<StepResult> execute(@NotNull ScenarioContext context) {
		String username = context.getUsername();
		String providerSubject = resolveProviderSubject(username);
		if (providerSubject == null)
			return CompletableFuture.completedFuture(StepResult.failed(""));

		CrackedMessages messages = messagesProvider.get();
		CrackedRegisterStateItem pending = getRegisterState(context);
		if (pending == null)
			return CompletableFuture.completedFuture(StepResult.waiting(joinRegisterPrompt(messages)));

		long ttlMs = registrationTtlMs();
		CrackedRegistrationAttempt input = consumeRegistrationAttempt(context, ttlMs);
		if (input == null)
			return CompletableFuture.completedFuture(StepResult.waiting(joinConfirmPrompt(messages)));

		if (!input.isConfirm()) {
			clearRegisterState(context, ttlMs);
			return CompletableFuture.completedFuture(StepResult.waiting(joinRegisterReset(messages)));
		}

		if (!matchesPending(input, pending)) {
			clearRegisterState(context, ttlMs);
			return CompletableFuture.completedFuture(StepResult.waiting(joinRegisterReset(messages)));
		}

		CrackedAccount account = accountService.register(
				providerSubject,
				pending.getPasswordHash(),
				pending.getHashingMethod(),
				PasswordChangeReason.REGISTER
		).orElse(null);
		clearRegisterState(context, ttlMs);
		if (account == null)
			return CompletableFuture.completedFuture(StepResult.failed(""));

		return CompletableFuture.completedFuture(complete(context, providerSubject, username));
	}

	private boolean matchesPending(CrackedRegistrationAttempt input, CrackedRegisterStateItem pending) {
		if (input == null || pending == null)
			return false;
		return cryptographyService.verify(
				input.getPassword(),
				pending.getPasswordHash(),
				pending.getHashingMethod()
		);
	}

	private String joinRegisterPrompt(CrackedMessages messages) {
		return joinLines(messages.getRegister().getPrompt());
	}

	private String joinConfirmPrompt(CrackedMessages messages) {
		return joinLines(messages.getRegister().getConfirmPrompt());
	}

	private String joinRegisterReset(CrackedMessages messages) {
		String mismatch = messages.getRegister().getMismatch();
		String prompt = joinRegisterPrompt(messages);
		if (mismatch == null || mismatch.isBlank())
			return prompt;
		return mismatch + "\n" + prompt;
	}

}
