package me.whereareiam.identica.type.step;

/**
 * Authentication flow type for a single authentication attempt.
 */
public enum AuthFlowType {
	/**
	 * Runs without player interaction.
	 */
	SEAMLESS,

	/**
	 * Allows player interaction steps.
	 */
	INTERACTIVE
}
