package me.whereareiam.identica.type.routing.reason;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Machine-readable reason for a rejected routing attempt.
 */
@Getter
@RequiredArgsConstructor
public enum RoutingAttemptFailureReason {
	MISSING_SERVER(false),
	CONNECTION_CANCELLED(false),
	CONNECTION_IN_PROGRESS(false),
	SERVER_DISCONNECTED(true),
	CONNECTION_EXCEPTION(true),
	CONNECTION_RESULT_MISSING(true);

	private final boolean retryable;
}
