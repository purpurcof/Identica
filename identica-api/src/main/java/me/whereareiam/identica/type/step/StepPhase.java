package me.whereareiam.identica.type.step;

/**
 * Phase of the authentication flow.
 */
public enum StepPhase {
	/**
	 * Global pre-selection phase.
	 */
	PRE,

	/**
	 * Provider-specific authentication phase.
	 */
	PROVIDER,

	/**
	 * Global post-authentication phase.
	 */
	END
}
