package me.whereareiam.identica.platform.adapter;

import org.jetbrains.annotations.NotNull;

/**
 * Required platform adapter role for applying profile preparation or rewrite
 * behavior.
 *
 * @param <E> platform event type
 */
public interface PlatformProfileAdapter<E> {
	/**
	 * Applies platform profile behavior to the given event.
	 *
	 * @param event platform event
	 */
	void apply(@NotNull E event);
}
