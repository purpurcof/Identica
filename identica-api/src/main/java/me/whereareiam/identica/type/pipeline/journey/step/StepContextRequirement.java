package me.whereareiam.identica.type.pipeline.journey.step;

/**
 * Minimum runtime context required before a journey step can execute.
 */
public enum StepContextRequirement {
	/**
	 * Profile/login context is enough; the player does not need to be attached.
	 */
	LOGIN,

	/**
	 * The player must be attached as an online identity.
	 */
	ONLINE
}
