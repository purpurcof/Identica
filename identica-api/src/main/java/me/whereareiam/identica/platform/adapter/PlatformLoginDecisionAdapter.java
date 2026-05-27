package me.whereareiam.identica.platform.adapter;

import org.jetbrains.annotations.NotNull;

/**
 * Required platform adapter role for processing login-time connection
 * decisions.
 *
 * @param <E> platform event type
 */
public interface PlatformLoginDecisionAdapter<E> {
	/**
	 * Processes a login event.
	 *
	 * @param event platform event
	 */
	void process(@NotNull E event);
}
