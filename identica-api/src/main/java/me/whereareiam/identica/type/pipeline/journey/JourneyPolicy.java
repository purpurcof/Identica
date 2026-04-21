package me.whereareiam.identica.type.pipeline.journey;

/**
 * Controls how strictly a configured journey mode is applied.
 */
public enum JourneyPolicy {
	/**
	 * Prefer the configured mode, then use another viable mode if needed.
	 */
	PREFER,

	/**
	 * Only use the configured mode.
	 */
	STRICT
}
