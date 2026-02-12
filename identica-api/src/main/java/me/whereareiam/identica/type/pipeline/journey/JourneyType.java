package me.whereareiam.identica.type.pipeline.journey;

/**
 * Stage/step flow type for a single connection scenario attempt.
 */
public enum JourneyType {
	/**
	 * Runs without player interaction.
	 */
	SEAMLESS,

	/**
	 * Allows player interaction steps.
	 */
	INTERACTIVE
}
