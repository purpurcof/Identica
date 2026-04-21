package me.whereareiam.identica.model.routing.attempt;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import me.whereareiam.identica.type.routing.RoutingRetryMode;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

/**
 * Controls how many routing attempts are allowed for an intent.
 */
@Getter
@Setter
@ToString
public class RoutingAttemptPolicy {
	private @NotNull RoutingRetryMode mode = RoutingRetryMode.RETRY_ONCE;
	private int maxAttempts = 1;
	private @NotNull Duration retryDelay = Duration.ZERO;
	private boolean consumeOnReached;
	private boolean consumeOnExhausted = true;

	public static @NotNull RoutingAttemptPolicy defaultStep() {
		RoutingAttemptPolicy policy = new RoutingAttemptPolicy();
		policy.setMode(RoutingRetryMode.RETRY_ONCE);
		policy.setConsumeOnReached(false);
		return policy;
	}

	public static @NotNull RoutingAttemptPolicy defaultCompletion() {
		RoutingAttemptPolicy policy = new RoutingAttemptPolicy();
		policy.setMode(RoutingRetryMode.NONE);
		policy.setConsumeOnReached(true);
		return policy;
	}

	public boolean allowsAttempt(int attemptsMade) {
		if (attemptsMade < 0) return false;

		return switch (mode) {
			case NONE -> attemptsMade < 1;
			case RETRY_ONCE -> attemptsMade < 2;
			case RETRY_LIMITED -> attemptsMade < Math.max(0, maxAttempts);
			case UNTIL_REACHED -> true;
		};
	}
}
