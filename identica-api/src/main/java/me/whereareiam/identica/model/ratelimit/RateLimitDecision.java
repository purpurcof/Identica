package me.whereareiam.identica.model.ratelimit;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import me.whereareiam.identica.ratelimit.RateLimitDefinition;

@Getter
@ToString
@RequiredArgsConstructor
public class RateLimitDecision {
	private final boolean limited;
	private final boolean deny;
	private final long remainingSeconds;
	private final String message;
	private final int remainingAttempts;
	private final String warningMessage;
	private final RateLimitDefinition definition;

	public static RateLimitDecision limited(
			RateLimitDefinition definition,
			long remainingSeconds,
			String message
	) {
		return new RateLimitDecision(true, true, remainingSeconds, message, 0, null, definition);
	}

	public static RateLimitDecision limited(
			RateLimitDefinition definition,
			long remainingSeconds,
			String message,
			boolean deny
	) {
		return new RateLimitDecision(true, deny, remainingSeconds, message, 0, null, definition);
	}

	public static RateLimitDecision allowed(RateLimitDefinition definition) {
		return new RateLimitDecision(false, false, 0L, null, 0, null, definition);
	}

	public static RateLimitDecision allowed(
			RateLimitDefinition definition,
			int remainingAttempts,
			String warningMessage
	) {
		return new RateLimitDecision(false, false, 0L, null, remainingAttempts, warningMessage, definition);
	}
}
