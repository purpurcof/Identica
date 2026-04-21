package me.whereareiam.identica.type.pipeline.journey.step;

/**
 * Describes why a journey step is waiting.
 */
public enum StepWaitReason {
	/**
	 * Waiting for player command/input.
	 */
	INPUT,

	/**
	 * Waiting for the player to be attached online.
	 */
	ONLINE
}
