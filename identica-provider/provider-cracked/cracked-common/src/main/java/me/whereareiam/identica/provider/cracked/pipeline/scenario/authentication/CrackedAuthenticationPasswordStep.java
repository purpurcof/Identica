package me.whereareiam.identica.provider.cracked.pipeline.scenario.authentication;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
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
import me.whereareiam.identica.provider.cracked.config.CrackedSettings;
import me.whereareiam.identica.provider.cracked.cryptography.CryptographyService;
import me.whereareiam.identica.provider.cracked.model.CrackedAccount;
import me.whereareiam.identica.provider.cracked.model.RateLimitResult;
import me.whereareiam.identica.provider.cracked.pipeline.CrackedAuthenticationAttempt;
import me.whereareiam.identica.provider.cracked.ratelimit.RateLimitService;
import me.whereareiam.identica.util.UniqueIdGenerator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Singleton
public class CrackedAuthenticationPasswordStep extends InteractiveStep {
	private final Provider<CrackedMessages> messagesProvider;
	private final Provider<CrackedSettings> settingsProvider;
	private final Provider<Settings> coreSettingsProvider;
	private final CrackedAccountService accountService;
	private final CryptographyService cryptographyService;
	private final PipelineStateStore pipelineStateStore;
	private final RateLimitService rateLimitService;

	@Inject
	public CrackedAuthenticationPasswordStep(
			Provider<CrackedMessages> messagesProvider,
			Provider<CrackedSettings> settingsProvider,
			Provider<Settings> coreSettingsProvider,
			CrackedAccountService accountService,
			CryptographyService cryptographyService,
			PipelineStateStore pipelineStateStore,
			RateLimitService rateLimitService
	) {
		super("cracked-authentication-password");
		this.messagesProvider = messagesProvider;
		this.settingsProvider = settingsProvider;
		this.coreSettingsProvider = coreSettingsProvider;
		this.accountService = accountService;
		this.cryptographyService = cryptographyService;
		this.pipelineStateStore = pipelineStateStore;
		this.rateLimitService = rateLimitService;
	}

	@Override
	public int order() {
		return 10;
	}

	@Override
	public @NotNull CompletableFuture<StepResult> execute(@NotNull ScenarioContext context) {
		String username = context.getUsername();
		String ip = context.getIp();
		String providerSubject = resolveProviderSubject(username);
		if (providerSubject == null)
			return CompletableFuture.completedFuture(StepResult.failed(""));

		CrackedMessages messages = messagesProvider.get();
		CrackedAccount account = accountService.find(providerSubject).orElse(null);
		if (account == null)
			return CompletableFuture.completedFuture(StepResult.denied(messages.getLogin().getNotRegistered()));

		long ttlMs = authenticationTtlMs();
		CrackedAuthenticationAttempt input = consumeAuthenticationAttempt(context, ttlMs);
		if (input == null)
			return CompletableFuture.completedFuture(StepResult.waiting(joinLines(messages.getLogin().getPrompt())));

		if (!cryptographyService.verify(account, input.getPassword())) {
			CrackedSettings settings = settingsProvider.get();
			CrackedSettings.Authentication authenticationSettings = settings != null
					&& settings.getScenario() != null
					? settings.getScenario().getAuthentication()
					: null;
			RateLimitResult result = rateLimitService.recordFailure(
					ip,
					authenticationSettings != null ? authenticationSettings.getMaxAttempts() : 0,
					authenticationSettings != null ? authenticationSettings.getLockSeconds() : 0
			);
			if (result.limited())
				return CompletableFuture.completedFuture(StepResult.denied(lockoutMessage(messages, result.remainingSeconds())));
			return CompletableFuture.completedFuture(StepResult.waiting(messages.getLogin().getInvalid()));
		}

		rateLimitService.clear(ip);
		return CompletableFuture.completedFuture(complete(context, providerSubject, username));
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

	private String lockoutMessage(CrackedMessages messages, long remainingSeconds) {
		if (messages.getLockout() == null)
			return "";
		return replaceTokens(messages.getLockout().getExceeded(),
				Map.of("seconds", String.valueOf(remainingSeconds)));
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

	private static @NotNull String replaceTokens(
			@Nullable List<String> lines,
			@NotNull Map<String, String> placeholders
	) {
		if (lines == null || lines.isEmpty())
			return "";

		List<String> rendered = new ArrayList<>(lines.size());
		for (String line : lines) {
			String out = line == null ? "" : line;
			for (Map.Entry<String, String> entry : placeholders.entrySet()) {
				String value = entry.getValue();
				out = out.replace("{" + entry.getKey() + "}", value == null ? "" : value);
			}
			rendered.add(out);
		}
		return String.join("\n", rendered);
	}
}
