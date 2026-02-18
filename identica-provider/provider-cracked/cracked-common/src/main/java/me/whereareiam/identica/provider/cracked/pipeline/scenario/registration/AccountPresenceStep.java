package me.whereareiam.identica.provider.cracked.pipeline.scenario.registration;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import me.whereareiam.identica.model.config.Settings;
import me.whereareiam.identica.model.pipeline.journey.stage.step.StepResult;
import me.whereareiam.identica.pipeline.ScenarioContext;
import me.whereareiam.identica.provider.cracked.account.CrackedAccountService;
import me.whereareiam.identica.provider.cracked.config.CrackedMessages;
import me.whereareiam.identica.provider.cracked.cryptography.CryptographyService;
import me.whereareiam.identica.provider.cracked.model.CrackedAccount;
import me.whereareiam.identica.provider.cracked.pipeline.CrackedAuthenticationAttempt;
import me.whereareiam.identica.provider.cracked.CrackedConstants;
import me.whereareiam.identica.ratelimit.RateLimitService;
import me.whereareiam.identica.model.ratelimit.RateLimitContext;
import me.whereareiam.identica.model.ratelimit.RateLimitDecision;
import me.whereareiam.identica.pipeline.state.PipelineStateStore;
import org.jetbrains.annotations.NotNull;
import java.util.concurrent.CompletableFuture;

@Singleton
public class AccountPresenceStep extends AbstractCrackedRegistrationStep {
	private final CrackedAccountService accountService;
	private final CryptographyService cryptographyService;
	private final RateLimitService rateLimitService;

	@Inject
	public AccountPresenceStep(
			Provider<CrackedMessages> messagesProvider,
			Provider<Settings> coreSettingsProvider,
			CrackedAccountService accountService,
			CryptographyService cryptographyService,
			PipelineStateStore pipelineStateStore,
			RateLimitService rateLimitService
	) {
		super("cracked-account-presence", messagesProvider, coreSettingsProvider, pipelineStateStore);
		this.accountService = accountService;
		this.cryptographyService = cryptographyService;
		this.rateLimitService = rateLimitService;
	}

	@Override
	public int order() {
		return 5;
	}

	@Override
	public @NotNull CompletableFuture<StepResult> execute(@NotNull ScenarioContext context) {
		String username = context.getUsername();
		String providerSubject = resolveProviderSubject(username);
		if (providerSubject == null)
			return CompletableFuture.completedFuture(StepResult.failed(""));

		CrackedAccount existing = accountService.find(providerSubject).orElse(null);
		if (existing == null)
			return CompletableFuture.completedFuture(StepResult.proceed(context));

		long ttlMs = registrationTtlMs();
		return CompletableFuture.completedFuture(handleExisting(context, existing, username, ttlMs));
	}

	private StepResult handleExisting(
			ScenarioContext context,
			CrackedAccount account,
			String username,
			long ttlMs
	) {
		CrackedMessages messages = messagesProvider.get();

		CrackedAuthenticationAttempt input = consumeAuthenticationAttempt(context, ttlMs);
		if (input == null) {
			String message = joinAlreadyRegistered(messages);
			return StepResult.waiting(message);
		}

		if (!cryptographyService.verify(account, input.getPassword())) {
			RateLimitDecision decision = rateLimitService.record(
					CrackedConstants.RATE_LIMIT.BRUTE_FORCE,
					rateLimitContext(context)
			);
			if (decision.isLimited() && decision.isDeny())
				return StepResult.denied(decision.getMessage());
			return invalidWithWarning(messages, decision);
		}

		rateLimitService.clear(CrackedConstants.RATE_LIMIT.BRUTE_FORCE, rateLimitContext(context));
		return complete(context, account.getProviderSubject(), username);
	}

	private String joinAlreadyRegistered(CrackedMessages messages) {
		String registered = messages.getScenario().getRegistration().getStatus().getAlreadyRegistered();
		String prompt = joinLines(messages.getScenario().getAuthentication().getPrompt());
		if (registered == null || registered.isBlank())
			return prompt;
		return registered + "\n" + prompt;
	}

	private RateLimitContext rateLimitContext(@NotNull ScenarioContext context) {
		return RateLimitContext.builder()
				.ip(context.getIp())
				.username(context.getUsername())
				.uniqueId(context.getIdenticaUniqueId())
				.connectionUniqueId(context.getConnectionUniqueId())
				.build();
	}

	private StepResult invalidWithWarning(CrackedMessages messages, RateLimitDecision decision) {
		String invalid = messages.getScenario().getAuthentication().getStatus().getInvalid();
		String warning = decision.getWarningMessage();

		if (warning == null || warning.isBlank())
			return StepResult.waiting(invalid);
		if (invalid == null || invalid.isBlank())
			return StepResult.waiting(warning);

		return StepResult.waiting(invalid + "\n" + warning);
	}
}
