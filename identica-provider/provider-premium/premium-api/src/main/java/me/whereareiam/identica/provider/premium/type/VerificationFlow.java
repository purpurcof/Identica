package me.whereareiam.identica.provider.premium.type;

/**
 * Controls how premium verification is initiated.
 */
public enum VerificationFlow {
	/**
	 * Always attempt premium verification immediately.
	 */
	SILENT,
	/**
	 * Ask the player before attempting premium verification.
	 */
	PROMPT
}
