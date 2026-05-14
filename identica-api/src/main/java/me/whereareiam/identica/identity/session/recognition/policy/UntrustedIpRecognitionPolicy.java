package me.whereareiam.identica.identity.session.recognition.policy;

import me.whereareiam.identica.model.provider.ProviderContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Evaluates whether automatic reconnect recognition should be suppressed for a connection.
 */
public interface UntrustedIpRecognitionPolicy {
	/**
	 * Evaluates the automatic reconnect-recognition decision for the given provider and connection.
	 *
	 * @param providerId provider being auto-considered
	 * @param clientIp current client IP
	 * @param selectedProvider current scenario provider context
	 * @return resolved recognition decision
	 */
	@NotNull UntrustedIpRecognitionDecision evaluateAutomaticRecognition(
			@Nullable String providerId,
			@Nullable String clientIp,
			@Nullable ProviderContext selectedProvider
	);

	/**
	 * Returns whether the given client IP matches a configured untrusted IP or CIDR entry.
	 *
	 * @param clientIp current client IP
	 * @return {@code true} when the IP is configured as untrusted
	 */
	boolean isUntrustedIp(@Nullable String clientIp);
}
