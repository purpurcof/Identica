package me.whereareiam.identica.type;

import org.jetbrains.annotations.NotNull;

/**
 * Stage of the generic connection preparation flow.
 *
 * <p>The platform may invoke preparation multiple times as more connection
 * information becomes available. For example, Velocity can run
 * {@link #HANDSHAKE} during pre-login and {@link #PROFILE} once the observed
 * game profile is available.</p>
 */
public enum PrepareStage {
	/**
	 * Early connection preparation before a platform profile is available.
	 */
	HANDSHAKE,

	/**
	 * Profile-aware preparation once the observed platform profile is available.
	 */
	PROFILE;

	/**
	 * Returns whether this stage includes profile-aware preparation.
	 *
	 * @return {@code true} when this stage can resolve profile data
	 */
	public boolean includesProfile() {
		return this == PROFILE;
	}

	/**
	 * Resolves a preparation stage or falls back to {@link #HANDSHAKE}.
	 *
	 * @param stage candidate stage
	 * @return resolved stage
	 */
	public static @NotNull PrepareStage resolve(PrepareStage stage) {
		return stage != null ? stage : HANDSHAKE;
	}
}
