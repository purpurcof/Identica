package me.whereareiam.identica.type.provider;

/**
 * Atomic condition that may allow a player through an active provider join restriction.
 */
public enum ProviderJoinRestrictionCondition {
	/**
	 * Allow reconnect-recognized players.
	 */
	RECOGNIZED,
	/**
	 * Allow players with an existing provider link.
	 */
	LINKED
}
