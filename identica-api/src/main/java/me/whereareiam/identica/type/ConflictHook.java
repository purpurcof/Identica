package me.whereareiam.identica.type;

/**
 * Default hook location for a conflict type.
 */
public enum ConflictHook {
	/**
	 * Resolve conflicts during account preparation.
	 */
	PREPARE,
	/**
	 * Do not apply automatically; caller must invoke resolution manually.
	 */
	NONE
}
