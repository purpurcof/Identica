package me.whereareiam.identica.provider.cracked.ratelimit;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.provider.cracked.CrackedConstants;
import me.whereareiam.identica.provider.cracked.config.CrackedMessages;
import me.whereareiam.identica.provider.cracked.config.CrackedSettings;
import me.whereareiam.identica.model.ratelimit.RateLimitContext;
import me.whereareiam.identica.model.ratelimit.RateLimitPolicy;
import me.whereareiam.identica.ratelimit.RateLimitDefinition;
import me.whereareiam.identica.type.ratelimit.RateLimitMode;
import me.whereareiam.identica.type.ratelimit.RateLimitScope;

import java.util.List;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class BruteForceRateLimitDefinition implements RateLimitDefinition {
	private static final RateLimitScope[] SCOPES = new RateLimitScope[]{
			RateLimitScope.PROCESS,
			RateLimitScope.RESUME,
			RateLimitScope.ADVANCE
	};

	private final Provider<CrackedSettings> settingsProvider;
	private final Provider<CrackedMessages> messagesProvider;

	@Override
	public String id() {
		return CrackedConstants.RATE_LIMIT.BRUTE_FORCE;
	}

	@Override
	public RateLimitScope[] scopes() {
		return SCOPES;
	}

	@Override
	public RateLimitMode modeFor(RateLimitScope scope) {
		return RateLimitMode.CHECK;
	}

	@Override
	public RateLimitPolicy policy(RateLimitContext ctx) {
		CrackedSettings settings = settingsProvider.get();
		CrackedSettings.Scenario.Authentication authentication = settings != null
				&& settings.getScenario() != null
				? settings.getScenario().getAuthentication()
				: null;
		CrackedSettings.Scenario.Authentication.Bruteforce bruteForce = authentication != null
				? authentication.getBruteforce()
				: null;

		RateLimitPolicy policy = new RateLimitPolicy();
		int maxAttempts = bruteForce != null ? bruteForce.getMaxAttempts() : 0;
		policy.setMaxAttempts(maxAttempts);
		policy.setEnabled(maxAttempts > 0);

		CrackedSettings.Scenario.Authentication.Bruteforce.Lockout lockout = bruteForce != null
				? bruteForce.getLockout()
				: null;

		RateLimitPolicy.Lockout policyLockout = policy.getLockout();
		if (policyLockout == null) {
			policyLockout = new RateLimitPolicy.Lockout();
			policy.setLockout(policyLockout);
		}

		policyLockout.setEnabled(lockout == null || lockout.isEnabled());
		policyLockout.setDuration(lockout != null ? lockout.getDuration() : null);
		policyLockout.setMessageSupplier((_, remainingSeconds) -> buildLockoutMessage(remainingSeconds));

		CrackedSettings.Scenario.Authentication.Bruteforce.Warning warning = bruteForce != null
				? bruteForce.getWarning()
				: null;

		RateLimitPolicy.Warning policyWarning = policy.getWarning();
		if (policyWarning == null) {
			policyWarning = new RateLimitPolicy.Warning();
			policy.setWarning(policyWarning);
		}

		policyWarning.setEnabled(warning != null && warning.isEnabled());
		policyWarning.setThresholdPercentage(warning != null ? warning.getThresholdPercentage() : 0);
		policyWarning.setMessageSupplier((_, remainingAttempts) -> buildWarningMessage(remainingAttempts));

		return policy;
	}

	private String buildLockoutMessage(long remainingSeconds) {
		CrackedMessages messages = messagesProvider.get();
		if (messages == null || messages.getScenario().getAuthentication().getBruteforce() == null) return "";

		List<String> lines = messages.getScenario().getAuthentication().getBruteforce().getExceeded();
		if (lines == null || lines.isEmpty()) return "";

		String seconds = String.valueOf(Math.max(0L, remainingSeconds));
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < lines.size(); i++) {
			String line = lines.get(i) == null ? "" : lines.get(i);
			if (i > 0) builder.append("\n");
			builder.append(line.replace("{seconds}", seconds));
		}

		return builder.toString();
	}

	private String buildWarningMessage(int remainingAttempts) {
		CrackedMessages messages = messagesProvider.get();
		if (messages == null || messages.getScenario().getAuthentication().getBruteforce() == null) return "";

		List<String> lines = messages.getScenario().getAuthentication().getBruteforce().getRemaining();
		if (lines == null || lines.isEmpty()) return "";

		String remaining = String.valueOf(Math.max(0, remainingAttempts));
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < lines.size(); i++) {
			String line = lines.get(i) == null ? "" : lines.get(i);
			if (i > 0) builder.append("\n");
			builder.append(line.replace("{remaining}", remaining));
		}

		return builder.toString();
	}
}
