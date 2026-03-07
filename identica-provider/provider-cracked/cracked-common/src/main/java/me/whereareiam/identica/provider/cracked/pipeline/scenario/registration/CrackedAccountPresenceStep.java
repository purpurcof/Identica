package me.whereareiam.identica.provider.cracked.pipeline.scenario.registration;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.model.config.Settings;
import me.whereareiam.identica.model.pipeline.journey.stage.step.StepResult;
import me.whereareiam.identica.pipeline.ScenarioContext;
import me.whereareiam.identica.provider.cracked.account.CrackedAccountService;
import me.whereareiam.identica.provider.cracked.config.CrackedMessages;
import me.whereareiam.identica.provider.cracked.cryptography.CryptographyService;
import me.whereareiam.identica.provider.cracked.event.authentication.AuthenticationAttemptDecision;
import me.whereareiam.identica.provider.cracked.event.authentication.AuthenticationAttemptFailedEvent;
import me.whereareiam.identica.provider.cracked.event.authentication.AuthenticationAttemptSucceededEvent;
import me.whereareiam.identica.provider.cracked.model.CrackedAccount;
import me.whereareiam.identica.provider.cracked.model.authentication.AuthenticationAttemptContext;
import me.whereareiam.identica.provider.cracked.pipeline.CrackedAuthenticationAttempt;
import me.whereareiam.identica.pipeline.state.PipelineStateStore;
import org.jetbrains.annotations.NotNull;
import java.util.concurrent.CompletableFuture;

@Singleton
public class CrackedAccountPresenceStep extends AbstractCrackedRegistrationStep {
	private final CrackedAccountService accountService;
	private final CryptographyService cryptographyService;
	private final EventManager eventManager;

	@Inject
	public CrackedAccountPresenceStep(
			Provider<CrackedMessages> messagesProvider,
			Provider<Settings> coreSettingsProvider,
			CrackedAccountService accountService,
			CryptographyService cryptographyService,
			PipelineStateStore pipelineStateStore,
			EventManager eventManager
	) {
		super("cracked-account-presence", messagesProvider, coreSettingsProvider, pipelineStateStore);
		this.accountService = accountService;
		this.cryptographyService = cryptographyService;
		this.eventManager = eventManager;
	}

	@Override
	public int order() {
		return 5;
	}

	@Override
	public @NotNull CompletableFuture<StepResult> execute(@NotNull ScenarioContext context) {
		String providerSubject = requireProviderSubject(context);

		CrackedAccount existing = accountService.find(providerSubject).orElse(null);
		if (existing == null)
			return CompletableFuture.completedFuture(StepResult.proceed(context));

		long ttlMs = registrationTtlMs();
		return CompletableFuture.completedFuture(handleExisting(context, existing, ttlMs));
	}

	private StepResult handleExisting(
			ScenarioContext context,
			CrackedAccount account,
			long ttlMs
	) {
		CrackedMessages messages = messagesProvider.get();

		CrackedAuthenticationAttempt input = consumeAuthenticationAttempt(context, ttlMs);
		if (input == null) {
			String message = joinAlreadyRegistered(messages);
			return StepResult.waiting(message);
		}

		if (!cryptographyService.verify(account, input.getPassword())) {
			AuthenticationAttemptDecision decision = recordBruteForceDecision(account, context);
			if (decision.isDeny()) return StepResult.denied(decision.getDenyMessage());

			return invalidWithWarning(messages, decision.getWarningMessage());
		}

		clearBruteForce(account, context);
		return StepResult.complete(context);
	}

	private @NotNull AuthenticationAttemptDecision recordBruteForceDecision(
			@NotNull CrackedAccount account,
			@NotNull ScenarioContext context
	) {
		AuthenticationAttemptFailedEvent event = new AuthenticationAttemptFailedEvent(
				attemptContext(account, context),
				null
		);
		eventManager.call(event);
		AuthenticationAttemptDecision decision = event.getDecision();
		return decision != null ? decision : AuthenticationAttemptDecision.allow();
	}

	private void clearBruteForce(
			@NotNull CrackedAccount account,
			@NotNull ScenarioContext context
	) {
		eventManager.call(new AuthenticationAttemptSucceededEvent(attemptContext(account, context)));
	}

	private @NotNull AuthenticationAttemptContext attemptContext(
			@NotNull CrackedAccount account,
			@NotNull ScenarioContext context
	) {
		return new AuthenticationAttemptContext(
				account,
				context.getConnectionUniqueId(),
				context.getIdenticaUniqueId(),
				context.getUsername(),
				context.getIp()
		);
	}

	private String joinAlreadyRegistered(CrackedMessages messages) {
		String registered = messages.getScenario().getRegistration().getStatus().getAlreadyRegistered();
		String prompt = joinLines(messages.getScenario().getAuthentication().getPrompt());
		if (registered == null || registered.isBlank())
			return prompt;
		return registered + "\n" + prompt;
	}

	private StepResult invalidWithWarning(CrackedMessages messages, String warning) {
		String invalid = messages.getScenario().getAuthentication().getStatus().getInvalid();
		if (warning == null || warning.isBlank())
			return StepResult.waiting(invalid);
		if (invalid == null || invalid.isBlank())
			return StepResult.waiting(warning);

		return StepResult.waiting(invalid + "\n" + warning);
	}
}
