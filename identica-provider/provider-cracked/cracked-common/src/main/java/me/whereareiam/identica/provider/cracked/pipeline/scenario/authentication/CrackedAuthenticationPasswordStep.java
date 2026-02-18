package me.whereareiam.identica.provider.cracked.pipeline.scenario.authentication;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.model.config.Settings;
import me.whereareiam.identica.model.pipeline.PipelineState;
import me.whereareiam.identica.model.pipeline.journey.stage.step.StepResult;
import me.whereareiam.identica.model.provider.ProviderContext;
import me.whereareiam.identica.pipeline.ScenarioContext;
import me.whereareiam.identica.pipeline.journey.step.type.InteractiveStep;
import me.whereareiam.identica.pipeline.state.PipelineStateReference;
import me.whereareiam.identica.pipeline.state.PipelineStateStore;
import me.whereareiam.identica.provider.cracked.CrackedConstants;
import me.whereareiam.identica.provider.cracked.account.CrackedAccountService;
import me.whereareiam.identica.provider.cracked.config.CrackedMessages;
import me.whereareiam.identica.provider.cracked.cryptography.CryptographyService;
import me.whereareiam.identica.provider.cracked.event.authentication.AuthenticationAttemptDecision;
import me.whereareiam.identica.provider.cracked.event.authentication.AuthenticationAttemptFailedEvent;
import me.whereareiam.identica.provider.cracked.event.authentication.AuthenticationAttemptSucceededEvent;
import me.whereareiam.identica.provider.cracked.model.CrackedAccount;
import me.whereareiam.identica.provider.cracked.model.authentication.AuthenticationAttemptContext;
import me.whereareiam.identica.provider.cracked.pipeline.CrackedAuthenticationAttempt;
import me.whereareiam.identica.util.UniqueIdGenerator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Singleton
public class CrackedAuthenticationPasswordStep extends InteractiveStep {
	private final Provider<CrackedMessages> messagesProvider;
	private final Provider<Settings> coreSettingsProvider;
	private final CrackedAccountService accountService;
	private final CryptographyService cryptographyService;
	private final PipelineStateStore pipelineStateStore;
	private final EventManager eventManager;

	@Inject
	public CrackedAuthenticationPasswordStep(
			Provider<CrackedMessages> messagesProvider,
			Provider<Settings> coreSettingsProvider,
			CrackedAccountService accountService,
			CryptographyService cryptographyService,
			PipelineStateStore pipelineStateStore,
			EventManager eventManager
	) {
		super("cracked-authentication-password");
		this.messagesProvider = messagesProvider;
		this.coreSettingsProvider = coreSettingsProvider;
		this.accountService = accountService;
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
		String username = context.getUsername();
		String providerSubject = resolveProviderSubject(username);
		if (providerSubject == null)
			return CompletableFuture.completedFuture(StepResult.failed(""));

		CrackedMessages messages = messagesProvider.get();
		CrackedAccount account = accountService.find(providerSubject).orElse(null);
		if (account == null)
			return CompletableFuture.completedFuture(StepResult.denied(messages.getScenario().getAuthentication().getStatus().getNotRegistered()));

		long ttlMs = authenticationTtlMs();
		CrackedAuthenticationAttempt input = consumeAuthenticationAttempt(context, ttlMs);
		if (input == null)
			return CompletableFuture.completedFuture(StepResult.waiting(joinLines(messages.getScenario().getAuthentication().getPrompt())));

		if (!cryptographyService.verify(account, input.getPassword())) {
			AuthenticationAttemptDecision decision = recordBruteForceDecision(account, context);
			if (decision.isDeny())
				return CompletableFuture.completedFuture(StepResult.denied(decision.getDenyMessage()));
			return CompletableFuture.completedFuture(invalidWithWarning(messages, decision.getWarningMessage()));
		}

		clearBruteForce(account, context);
		return CompletableFuture.completedFuture(complete(context, providerSubject, username));
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

	private StepResult complete(ScenarioContext context, String providerSubject, String username) {
		ProviderContext provider = ProviderContext.builder()
				.providerId(CrackedConstants.PROVIDER_ID)
				.providerSubject(providerSubject)
				.providerUsername(username == null ? "" : username)
				.build();
		context.setProvider(provider);
		return StepResult.complete(context);
	}

	private String resolveProviderSubject(String username) {
		UUID uuid = UniqueIdGenerator.offlinePlayerUniqueId(username);
		return uuid != null ? uuid.toString() : null;
	}

	private long authenticationTtlMs() {
		Settings settings = coreSettingsProvider.get();
		if (settings == null)
			return 0L;
		return settings.getConnection().getAuthentication().pipelineTtlMillis();
	}

	private @NotNull PipelineStateReference reference(@NotNull ScenarioContext context) {
		return PipelineStateReference.from(context);
	}

	private @Nullable CrackedAuthenticationAttempt consumeAuthenticationAttempt(
			@NotNull ScenarioContext context,
			long ttlMs
	) {
		PipelineStateReference reference = reference(context);
		PipelineState stored = pipelineStateStore.find(reference).orElse(null);
		if (stored == null) return null;

		CrackedAuthenticationAttempt input = stored.item(CrackedAuthenticationAttempt.class).orElse(null);
		if (input == null) return null;

		stored.removeItem(CrackedAuthenticationAttempt.class);
		if (ttlMs > 0) {
			pipelineStateStore.save(reference, stored, ttlMs);
		}
		return input;
	}

	private static @NotNull String joinLines(@Nullable List<String> lines) {
		if (lines == null || lines.isEmpty())
			return "";
		return String.join("\n", lines);
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
