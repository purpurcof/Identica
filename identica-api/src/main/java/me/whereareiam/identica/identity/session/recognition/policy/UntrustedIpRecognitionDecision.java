package me.whereareiam.identica.identity.session.recognition.policy;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

/**
 * Result of evaluating whether automatic reconnect recognition may proceed for a connection.
 */
@Getter
@ToString
@AllArgsConstructor
public class UntrustedIpRecognitionDecision {
	private final @NotNull Outcome outcome;

	/**
	 * Returns whether automatic reconnect recognition must be blocked.
	 *
	 * @return {@code true} when automatic recognition should be suppressed
	 */
	public boolean isBlocked() {
		return outcome == Outcome.BLOCKED_UNTRUSTED_IP;
	}

	/**
	 * Outcome of untrusted-IP recognition evaluation.
	 */
	public enum Outcome {
		ALLOWED_DISABLED,
		ALLOWED_MISSING_IP,
		ALLOWED_IP_NOT_MATCHED,
		ALLOWED_PROVIDER_OVERRIDE,
		ALLOWED_EXPLICIT_SELECTION,
		BLOCKED_UNTRUSTED_IP
	}
}
