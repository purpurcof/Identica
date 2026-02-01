package me.whereareiam.identica.type.step;

/**
 * Defines which audience should see a step.
 */
public enum StepAudience {
	/**
	 * Step runs for all players.
	 */
	ALL,
	/**
	 * Step runs only for players without existing provider links.
	 */
	NEW_PLAYERS,
	/**
	 * Step runs only for players with existing provider links.
	 */
	EXISTING_PLAYERS
}
