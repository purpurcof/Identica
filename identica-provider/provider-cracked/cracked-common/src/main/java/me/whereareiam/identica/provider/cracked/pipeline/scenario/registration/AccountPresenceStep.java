package me.whereareiam.identica.provider.cracked.pipeline.scenario.registration;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import me.whereareiam.identica.model.config.Settings;
import me.whereareiam.identica.model.pipeline.journey.stage.step.StepResult;
import me.whereareiam.identica.pipeline.ScenarioContext;
import me.whereareiam.identica.provider.cracked.account.CrackedAccountService;
import me.whereareiam.identica.provider.cracked.config.CrackedMessages;
import me.whereareiam.identica.provider.cracked.config.CrackedSettings;
import me.whereareiam.identica.provider.cracked.cryptography.CryptographyService;
import me.whereareiam.identica.provider.cracked.model.CrackedAccount;
import me.whereareiam.identica.provider.cracked.model.RateLimitResult;
import me.whereareiam.identica.provider.cracked.pipeline.CrackedAuthenticationAttempt;
import me.whereareiam.identica.provider.cracked.ratelimit.RateLimitService;
import me.whereareiam.identica.pipeline.state.PipelineStateStore;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Singleton
public class AccountPresenceStep extends AbstractCrackedRegistrationStep {
	private final Provider<CrackedSettings> settingsProvider;
	private final CrackedAccountService accountService;
	private final CryptographyService cryptographyService;
	private final RateLimitService rateLimitService;

	@Inject
	public AccountPresenceStep(
			Provider<CrackedMessages> messagesProvider,
			Provider<CrackedSettings> settingsProvider,
			Provider<Settings> coreSettingsProvider,
			CrackedAccountService accountService,
			CryptographyService cryptographyService,
			PipelineStateStore pipelineStateStore,
			RateLimitService rateLimitService
	) {
		super("cracked-account-presence", messagesProvider, coreSettingsProvider, pipelineStateStore);
		this.settingsProvider = settingsProvider;
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
		String ip = context.getIp();
		String providerSubject = resolveProviderSubject(username);
		if (providerSubject == null)
			return CompletableFuture.completedFuture(StepResult.failed(""));

		CrackedAccount existing = accountService.find(providerSubject).orElse(null);
		if (existing == null)
			return CompletableFuture.completedFuture(StepResult.proceed(context));

		long ttlMs = registrationTtlMs();
		return CompletableFuture.completedFuture(handleExisting(context, existing, username, ip, ttlMs));
	}

	private StepResult handleExisting(
			ScenarioContext context,
			CrackedAccount account,
			String username,
			String ip,
			long ttlMs
	) {
		CrackedMessages messages = messagesProvider.get();
		CrackedSettings settings = settingsProvider.get();
		CrackedSettings.Authentication authenticationSettings = settings != null
				&& settings.getScenario() != null
				? settings.getScenario().getAuthentication()
				: null;
		long remaining = rateLimitService.remainingLimitSeconds(ip);
		if (remaining > 0)
			return StepResult.denied(lockoutMessage(messages, remaining));

		CrackedAuthenticationAttempt input = consumeAuthenticationAttempt(context, ttlMs);
		if (input == null) {
			String message = joinAlreadyRegistered(messages);
			return StepResult.waiting(message);
		}

		if (!cryptographyService.verify(account, input.getPassword())) {
			RateLimitResult result = rateLimitService.recordFailure(
					ip,
					authenticationSettings != null ? authenticationSettings.getMaxAttempts() : 0,
					authenticationSettings != null ? authenticationSettings.getLockSeconds() : 0
			);
			if (result.limited())
				return StepResult.denied(lockoutMessage(messages, result.remainingSeconds()));
			return StepResult.waiting(messages.getLogin().getInvalid());
		}

		rateLimitService.clear(ip);
		return complete(context, account.getProviderSubject(), username);
	}

	private String joinAlreadyRegistered(CrackedMessages messages) {
		String registered = messages.getRegister().getAlreadyRegistered();
		String prompt = joinLines(messages.getLogin().getPrompt());
		if (registered == null || registered.isBlank())
			return prompt;
		return registered + "\n" + prompt;
	}

	private String lockoutMessage(CrackedMessages messages, long remainingSeconds) {
		if (messages.getLockout() == null)
			return "";
		return replaceTokens(messages.getLockout().getExceeded(),
				Map.of("seconds", String.valueOf(remainingSeconds)));
	}
}
