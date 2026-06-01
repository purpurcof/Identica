package me.whereareiam.identica.type.provider;

/**
 * Atomic condition that may allow a player through an active provider join restriction.
 */
public enum ProviderJoinRestrictionCondition {
	/**
	 * Allow reconnect-recognized players.
	 *
	 * <p>TODO restore runtime support after join-restriction allowance
	 * evaluation is contributed by capability modules instead of core.</p>
	 */
	RECOGNIZED,
	/**
	 * Allow players with an existing provider link.
	 */
	LINKED
}
