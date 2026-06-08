package me.whereareiam.identica.conflict;

import org.jetbrains.annotations.NotNull;

/**
 * Generic subject presented to a conflict type.
 */
public interface ConflictSubject {
	/**
	 * Returns the hook where the conflict is being evaluated.
	 *
	 * @return hook id
	 */
	@NotNull String hook();
}
