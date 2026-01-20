package me.whereareiam.identica.type;

/**
 * Step type determines whether routing/player interaction is required.
 */
public enum AuthStepType {
	/**
	 * Seamless step - executes automatically.
	 */
	SEAMLESS,

	/**
	 * Interactive step - requires player action.
	 */
	INTERACTIVE
}
