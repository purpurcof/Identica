package me.whereareiam.identica.type.identity;

/**
 * Strategy used when Identica assigns a unique id to a newly discovered account.
 */
public enum UniqueIdMode {
	/**
	 * Generate an Identica-owned random UUID.
	 */
	RANDOM,

	/**
	 * Derive the UUID from the player's offline-mode username.
	 */
	OFFLINE,

	/**
	 * Reuse the premium/provider UUID when it is available.
	 */
	PREMIUM
}
