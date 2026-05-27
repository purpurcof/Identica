package me.whereareiam.identica.type;

/**
 * Resolution reason for a held scenario state.
 */
public enum ScenarioResolution {
	/**
	 * The scenario completed successfully and opened the intended live session.
	 */
	COMPLETED,
	/**
	 * The scenario ended because processing failed.
	 */
	FAILED,
	/**
	 * The scenario ended because processing was denied by policy or validation.
	 */
	DENIED,
	/**
	 * The scenario ended because it was cancelled explicitly or cleared by an
	 * external lifecycle action.
	 */
	CANCELLED,
	/**
	 * The scenario ended because its held pending state expired.
	 */
	EXPIRED
}
