package me.whereareiam.identica.type.routing;

/**
 * Retry policy for routing attempts.
 */
public enum RoutingRetryMode {
	NONE,
	RETRY_ONCE,
	RETRY_LIMITED,
	UNTIL_REACHED
}
