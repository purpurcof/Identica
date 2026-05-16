package me.whereareiam.identica.type.messaging;

/**
 * Origin of a queued delivery request.
 */
public enum DeliverySource {
	/**
	 * Delivery produced by an initial pipeline prompt or wait state.
	 */
	INITIAL_PROMPT,
	/**
	 * Delivery used to resume pipeline completion once the player is ready.
	 */
	COMPLETION,
	/**
	 * Delivery used for informational notices outside the prompt/completion flow.
	 */
	NOTICE
}
