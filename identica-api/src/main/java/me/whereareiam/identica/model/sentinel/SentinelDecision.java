package me.whereareiam.identica.model.sentinel;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import me.whereareiam.identica.sentinel.SentinelDefinition;

@Getter
@ToString
@RequiredArgsConstructor
public class SentinelDecision {
	private final boolean limited;
	private final boolean deny;
	private final long remainingSeconds;
	private final String message;
	private final int remainingAttempts;
	private final String warningMessage;
	private final SentinelDefinition definition;

	public static SentinelDecision limited(
			SentinelDefinition definition,
			long remainingSeconds,
			String message
	) {
		return new SentinelDecision(true, true, remainingSeconds, message, 0, null, definition);
	}

	public static SentinelDecision limited(
			SentinelDefinition definition,
			long remainingSeconds,
			String message,
			boolean deny
	) {
		return new SentinelDecision(true, deny, remainingSeconds, message, 0, null, definition);
	}

	public static SentinelDecision allowed(SentinelDefinition definition) {
		return new SentinelDecision(false, false, 0L, null, 0, null, definition);
	}

	public static SentinelDecision allowed(
			SentinelDefinition definition,
			int remainingAttempts,
			String warningMessage
	) {
		return new SentinelDecision(false, false, 0L, null, remainingAttempts, warningMessage, definition);
	}
}
