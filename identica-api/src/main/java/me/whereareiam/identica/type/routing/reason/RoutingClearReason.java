package me.whereareiam.identica.type.routing.reason;

/**
 * Reason a routing intent was cleared.
 */
public enum RoutingClearReason {
	NO_TARGET,
	PIPELINE_FAILED,
	PENDING_CLEARED,
	DISCONNECT,
	REACHED,
	EXHAUSTED,
	REPLACED,
	MANUAL
}
