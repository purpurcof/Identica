package me.whereareiam.identica.platform.adapter;

import org.jetbrains.annotations.NotNull;

/**
 * Required platform adapter role for processing first-connect resume
 * decisions.
 *
 * @param <E> platform event type
 */
public interface PlatformResumeDecisionAdapter<E> {
	/**
	 * Processes a resume event.
	 *
	 * @param event platform event
	 */
	void resume(@NotNull E event);
}
