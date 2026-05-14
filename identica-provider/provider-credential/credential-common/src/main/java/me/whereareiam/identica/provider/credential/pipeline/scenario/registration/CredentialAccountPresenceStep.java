package me.whereareiam.identica.provider.credential.pipeline.scenario.registration;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.model.config.Settings;
import me.whereareiam.identica.model.pipeline.journey.stage.step.StepResult;
import me.whereareiam.identica.pipeline.ScenarioContext;
import me.whereareiam.identica.pipeline.state.PipelineStateStore;
import me.whereareiam.identica.provider.credential.account.CredentialAccountService;
import me.whereareiam.identica.provider.credential.config.CredentialMessages;
import me.whereareiam.identica.provider.credential.cryptography.CryptographyService;
import me.whereareiam.identica.provider.credential.event.authentication.AuthenticationAttemptDecision;
import me.whereareiam.identica.provider.credential.event.authentication.AuthenticationAttemptFailedEvent;
import me.whereareiam.identica.provider.credential.event.authentication.AuthenticationAttemptSucceededEvent;
import me.whereareiam.identica.provider.credential.model.CredentialAccount;
import me.whereareiam.identica.provider.credential.model.authentication.AuthenticationAttemptContext;
import me.whereareiam.identica.provider.credential.pipeline.CredentialAuthenticationAttempt;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

@Singleton
public class CredentialAccountPresenceStep extends AbstractCredentialRegistrationStep {
	private final CredentialAccountService credentialService;
	private final CryptographyService cryptographyService;
	private final EventManager eventManager;

	@Inject
	public CredentialAccountPresenceStep(
			Provider<CredentialMessages> messagesProvider,
			Provider<Settings> coreSettingsProvider,
			CredentialAccountService credentialService,
			CryptographyService cryptographyService,
			PipelineStateStore pipelineStateStore,
			EventManager eventManager
	) {
		super("password-credential-presence", messagesProvider, coreSettingsProvider, pipelineStateStore);
		this.credentialService = credentialService;
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

		CredentialAccount existing = credentialService.find(providerSubject).orElse(null);
		if (existing == null)
			return CompletableFuture.completedFuture(StepResult.proceed(context));

		long ttlMs = registrationTtlMs();
		return CompletableFuture.completedFuture(handleExisting(context, existing, ttlMs));
	}

	private StepResult handleExisting(
			ScenarioContext context,
			CredentialAccount credential,
			long ttlMs
	) {
		CredentialMessages messages = messagesProvider.get();

		CredentialAuthenticationAttempt input = consumeAuthenticationAttempt(context, ttlMs);
		if (input == null) {
			String message = joinAlreadyRegistered(messages);
			return StepResult.waiting(message);
		}

		if (!cryptographyService.verify(credential, input.getPassword())) {
			AuthenticationAttemptDecision decision = recordBruteForceDecision(credential, context);
			if (decision.isDeny()) return StepResult.denied(decision.getDenyMessage());

			return invalidWithWarning(messages, decision.getWarningMessage());
		}

		clearBruteForce(credential, context);
		return StepResult.complete(context);
	}

	private @NotNull AuthenticationAttemptDecision recordBruteForceDecision(
			@NotNull CredentialAccount credential,
			@NotNull ScenarioContext context
	) {
		AuthenticationAttemptFailedEvent event = new AuthenticationAttemptFailedEvent(
				attemptContext(credential, context),
				null
		);
		eventManager.call(event);
		AuthenticationAttemptDecision decision = event.getDecision();
		return decision != null
				? decision
				: AuthenticationAttemptDecision.allow();
	}

	private void clearBruteForce(
			@NotNull CredentialAccount credential,
			@NotNull ScenarioContext context
	) {
		eventManager.call(new AuthenticationAttemptSucceededEvent(attemptContext(credential, context)));
	}

	private @NotNull AuthenticationAttemptContext attemptContext(
			@NotNull CredentialAccount credential,
			@NotNull ScenarioContext context
	) {
		return new AuthenticationAttemptContext(
				credential,
				context.getConnectionUniqueId(),
				context.getAccountUniqueId(),
				context.getUsername(),
				context.getIp()
		);
	}

	private String joinAlreadyRegistered(CredentialMessages messages) {
		String registered = messages.getScenario().getRegistration().getStatus().getAlreadyRegistered();
		String prompt = joinLines(messages.getScenario().getAuthentication().getPrompt());
		if (registered == null || registered.isBlank())
			return prompt;

		return registered + "\n" + prompt;
	}

	private StepResult invalidWithWarning(CredentialMessages messages, String warning) {
		String invalid = messages.getScenario().getAuthentication().getStatus().getInvalid();
		if (warning == null || warning.isBlank()) return StepResult.waiting(invalid);
		if (invalid == null || invalid.isBlank()) return StepResult.waiting(warning);

		return StepResult.waiting(invalid + "\n" + warning);
	}
}
