package me.whereareiam.identica.common.sentinel;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.model.config.Messages;
import me.whereareiam.identica.model.config.Settings;
import me.whereareiam.identica.model.sentinel.SentinelContext;
import me.whereareiam.identica.model.sentinel.SentinelPolicy;
import me.whereareiam.identica.sentinel.SentinelDefinition;
import me.whereareiam.identica.type.sentinel.SentinelMode;
import me.whereareiam.identica.type.sentinel.SentinelScope;

import java.util.List;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ResumeSpamSentinelDefinition implements SentinelDefinition {
	private static final SentinelScope[] SCOPES = new SentinelScope[]{
			SentinelScope.PROCESS,
			SentinelScope.RESUME,
			SentinelScope.ADVANCE
	};

	private final Provider<Settings> settingsProvider;
	private final Provider<Messages> messagesProvider;

	@Override
	public String id() {
		return "resume-spam";
	}

	@Override
	public SentinelScope[] scopes() {
		return SCOPES;
	}

	@Override
	public SentinelMode modeFor(SentinelScope scope) {
		return SentinelMode.RECORD;
	}

	@Override
	public SentinelPolicy policy(SentinelContext ctx) {
		SentinelPolicy policy = settingsProvider.get().getConnection().getSentinels().getResumeSpam();

		if (policy.getLockout() == null) {
			policy.setLockout(new SentinelPolicy.Lockout());
		}

		policy.getLockout().setMessageSupplier((ignored, remainingSeconds) -> buildMessage(remainingSeconds));

		return policy;
	}

	private String buildMessage(long remainingSeconds) {
		Messages messages = messagesProvider.get();
		List<String> lines = messages.getConnection().getResumeSentineled();

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
