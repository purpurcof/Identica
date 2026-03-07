package me.whereareiam.identica.type;

/**
 * Controls how aggressively Identica re-applies resolved routing targets when
 * other proxy plugins override the target server.
 */
public enum RoutingEnforcementMode {
	/**
	 * Never re-apply the routing target after Identica's normal event handlers run.
	 */
	OFF,
	/**
	 * Re-apply the pending routing target once after the first backend connection.
	 * This is the safe compatibility mode for proxy fallback plugins.
	 */
	FIRST_CONNECT,
	/**
	 * Re-apply the pending routing target after every backend connect while the
	 * target is still pending.
	 */
	STRICT
}
