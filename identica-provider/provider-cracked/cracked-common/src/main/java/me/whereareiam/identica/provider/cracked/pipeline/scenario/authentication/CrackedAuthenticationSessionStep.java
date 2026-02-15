package me.whereareiam.identica.provider.cracked.pipeline.scenario.authentication;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import me.whereareiam.identica.identity.session.SessionService;
import me.whereareiam.identica.model.Session;
import me.whereareiam.identica.model.pipeline.journey.stage.step.StepResult;
import me.whereareiam.identica.model.provider.ProviderContext;
import me.whereareiam.identica.pipeline.ScenarioContext;
import me.whereareiam.identica.pipeline.journey.step.type.InteractiveStep;
import me.whereareiam.identica.provider.cracked.CrackedConstants;
import me.whereareiam.identica.provider.cracked.config.CrackedMessages;
import me.whereareiam.identica.provider.cracked.config.CrackedSettings;
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
public class CrackedAuthenticationSessionStep extends InteractiveStep {
	private final Provider<CrackedMessages> messagesProvider;
	private final Provider<CrackedSettings> settingsProvider;
	private final SessionService sessionService;
	private final RateLimitService rateLimitService;

	@Inject
	public CrackedAuthenticationSessionStep(
			Provider<CrackedMessages> messagesProvider,
			Provider<CrackedSettings> settingsProvider,
			SessionService sessionService,
			RateLimitService rateLimitService
	) {
		super("cracked-authentication-session");
		this.messagesProvider = messagesProvider;
		this.settingsProvider = settingsProvider;
		this.sessionService = sessionService;
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

		CrackedMessages messages = messagesProvider.get();
		long remaining = rateLimitService.remainingLimitSeconds(ip);
		if (remaining > 0)
			return CompletableFuture.completedFuture(StepResult.denied(lockoutMessage(messages, remaining)));

		CrackedSettings settings = settingsProvider.get();
		CrackedSettings.Authentication authenticationSettings = settings != null
				&& settings.getScenario() != null
				? settings.getScenario().getAuthentication()
				: null;

		if (isSessionValid(authenticationSettings, providerSubject, ip))
			return CompletableFuture.completedFuture(complete(context, providerSubject, username));

		return CompletableFuture.completedFuture(StepResult.proceed(context));
	}

	private boolean isSessionValid(CrackedSettings.Authentication authenticationSettings, String providerSubject, String ip) {
		if (authenticationSettings == null || authenticationSettings.getSession() == null)
			return false;

		long autoLoginSeconds = authenticationSettings.getSession().getAutoLoginSeconds();
		if (autoLoginSeconds <= 0)
			return false;

		Session session = sessionService.findByProviderSubject(CrackedConstants.PROVIDER_ID, providerSubject)
				.join()
				.orElse(null);
		if (session == null)
			return false;

		long now = System.currentTimeMillis();
		if (session.getCreatedAt() + autoLoginSeconds * 1000L < now)
			return false;

		if (authenticationSettings.getSession().isRequireSameIp()) {
			String sessionIp = session.getIp();
			return sessionIp != null && sessionIp.equals(ip);
		}

		return true;
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

	private String lockoutMessage(CrackedMessages messages, long remainingSeconds) {
		if (messages.getLockout() == null)
			return "";
		return replaceTokens(messages.getLockout().getExceeded(),
				Map.of("seconds", String.valueOf(remainingSeconds)));
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
