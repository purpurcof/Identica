package me.whereareiam.identica.common.ratelimit;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.model.config.Messages;
import me.whereareiam.identica.model.config.Settings;
import me.whereareiam.identica.model.ratelimit.RateLimitContext;
import me.whereareiam.identica.model.ratelimit.RateLimitPolicy;
import me.whereareiam.identica.ratelimit.RateLimitDefinition;
import me.whereareiam.identica.type.ratelimit.RateLimitMode;
import me.whereareiam.identica.type.ratelimit.RateLimitScope;

import java.util.List;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ResumeSpamRateLimitDefinition implements RateLimitDefinition {
	private static final RateLimitScope[] SCOPES = new RateLimitScope[]{
			RateLimitScope.PROCESS,
			RateLimitScope.RESUME,
			RateLimitScope.ADVANCE
	};

	private final Provider<Settings> settingsProvider;
	private final Provider<Messages> messagesProvider;

	@Override
	public String id() {
		return "resume-spam";
	}

	@Override
	public RateLimitScope[] scopes() {
		return SCOPES;
	}

	@Override
	public RateLimitMode modeFor(RateLimitScope scope) {
		return RateLimitMode.RECORD;
	}

	@Override
	public RateLimitPolicy policy(RateLimitContext ctx) {
		RateLimitPolicy policy = settingsProvider.get().getConnection().getRateLimits().getResumeSpam();

		if (policy.getLockout() == null) {
			policy.setLockout(new RateLimitPolicy.Lockout());
		}

		policy.getLockout().setMessageSupplier((_, remainingSeconds) -> buildMessage(remainingSeconds));

		return policy;
	}

	private String buildMessage(long remainingSeconds) {
		Messages messages = messagesProvider.get();
		List<String> lines = messages.getConnection().getResumeRateLimited();

		String seconds = String.valueOf(Math.max(0L, remainingSeconds));
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < lines.size(); i++) {
			String line = lines.get(i) == null ? "" : lines.get(i);
			if (i > 0) builder.append("\n");
			builder.append(line.replace("{seconds}", seconds));
		}

		return builder.toString();
	}
}
