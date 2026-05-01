package me.whereareiam.identica.type.routing;

/**
 * Platform event that triggered a routing attempt.
 */
public enum RoutingAttemptTrigger {
	INITIAL_SERVER,
	PRE_CONNECT,
	ASYNC_CONNECT,
	SCHEDULED_RETRY
}
