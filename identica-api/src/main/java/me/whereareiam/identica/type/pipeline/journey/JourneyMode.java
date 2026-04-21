package me.whereareiam.identica.type.pipeline.journey;

/**
 * Stage/step journey mode type for a single connection scenario attempt.
 */
public enum JourneyMode {
	/**
	 * Runs without player interaction.
	 */
	SEAMLESS,

	/**
	 * Allows player interaction steps.
	 */
	INTERACTIVE
}
