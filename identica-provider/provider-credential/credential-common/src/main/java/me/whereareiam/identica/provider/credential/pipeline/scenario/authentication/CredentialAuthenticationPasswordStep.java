package me.whereareiam.identica.provider.credential.pipeline.scenario.authentication;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.model.config.Settings;
import me.whereareiam.identica.model.pipeline.journey.stage.step.StepResult;
import me.whereareiam.identica.model.pipeline.state.PipelineState;
import me.whereareiam.identica.model.pipeline.state.PipelineStateReference;
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
import me.whereareiam.identica.provider.credential.pipeline.scenario.AbstractCredentialStep;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Singleton
public class CredentialAuthenticationPasswordStep extends AbstractCredentialStep {
	private final Provider<CredentialMessages> messagesProvider;
	private final Provider<Settings> coreSettingsProvider;
	private final CredentialAccountService credentialService;
	private final CryptographyService cryptographyService;
	private final PipelineStateStore pipelineStateStore;
	private final EventManager eventManager;

	@Inject
	public CredentialAuthenticationPasswordStep(
			Provider<CredentialMessages> messagesProvider,
			Provider<Settings> coreSettingsProvider,
			CredentialAccountService credentialService,
			CryptographyService cryptographyService,
			PipelineStateStore pipelineStateStore,
			EventManager eventManager
	) {
		super("password-authentication-password");
		this.messagesProvider = messagesProvider;
		this.coreSettingsProvider = coreSettingsProvider;
		this.credentialService = credentialService;
		this.cryptographyService = cryptographyService;
		this.pipelineStateStore = pipelineStateStore;
		this.eventManager = eventManager;
	}

	@Override
	public int order() {
		return 10;
	}

	@Override
	public @NotNull CompletableFuture<StepResult> execute(@NotNull ScenarioContext context) {
		String providerSubject = requireProviderSubject(context);

		CredentialMessages messages = messagesProvider.get();
		CredentialAccount credential = credentialService.find(providerSubject).orElse(null);
		if (credential == null) return CompletableFuture.completedFuture(StepResult.denied(messages.getScenario().getAuthentication().getStatus().getNotRegistered()));

		long ttlMs = authenticationTtlMs();
		CredentialAuthenticationAttempt input = consumeAuthenticationAttempt(context, ttlMs);
		if (input == null) return CompletableFuture.completedFuture(StepResult.waiting(joinLines(messages.getScenario().getAuthentication().getPrompt())));

		if (!cryptographyService.verify(credential, input.getPassword())) {
			AuthenticationAttemptDecision decision = recordBruteForceDecision(credential, context);
			if (decision.isDeny()) return CompletableFuture.completedFuture(StepResult.denied(decision.getDenyMessage()));
			return CompletableFuture.completedFuture(invalidWithWarning(messages, decision.getWarningMessage()));
		}

		clearBruteForce(credential, context);
		return CompletableFuture.completedFuture(StepResult.proceed(context));
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
		return decision != null ? decision : AuthenticationAttemptDecision.allow();
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

	private long authenticationTtlMs() {
		Settings settings = coreSettingsProvider.get();
		if (settings == null) return 0L;
		return settings.getConnection().getScenarios().getAuthentication().pipelineTtlMillis();
	}

	private @NotNull PipelineStateReference reference(@NotNull ScenarioContext context) {
		return PipelineStateReference.from(context);
	}

	private @Nullable CredentialAuthenticationAttempt consumeAuthenticationAttempt(
			@NotNull ScenarioContext context,
			long ttlMs
	) {
		PipelineStateReference reference = reference(context);
		PipelineState stored = pipelineStateStore.find(reference).orElse(null);
		if (stored == null) return null;

		CredentialAuthenticationAttempt input = stored.item(CredentialAuthenticationAttempt.class).orElse(null);
		if (input == null) return null;

		stored.removeItem(CredentialAuthenticationAttempt.class);
		if (ttlMs > 0) {
			pipelineStateStore.save(reference, stored, ttlMs);
		}
		return input;
	}

	private static @NotNull String joinLines(@Nullable List<String> lines) {
		if (lines == null || lines.isEmpty()) return "";
		return String.join("\n", lines);
	}

	private StepResult invalidWithWarning(CredentialMessages messages, String warning) {
		String invalid = messages.getScenario().getAuthentication().getStatus().getInvalid();
		if (warning == null || warning.isBlank()) return StepResult.waiting(invalid);
		if (invalid == null || invalid.isBlank()) return StepResult.waiting(warning);

		return StepResult.waiting(invalid + "\n" + warning);
	}
}
