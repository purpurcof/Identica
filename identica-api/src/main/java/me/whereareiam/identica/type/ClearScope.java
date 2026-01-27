package me.whereareiam.identica.type;

/**
 * Scope for clearing account/session data.
 */
public enum ClearScope {
	/**
	 * Clear cached session data only.
	 */
	CACHE,
	/**
	 * Clear all persisted data for the identity.
	 */
	ALL
}
