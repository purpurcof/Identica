package me.whereareiam.identica.platform.adapter;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletionStage;

/**
 * Required platform adapter role for processing handshake-time connection
 * decisions.
 *
 * @param <E> platform event type
 */
public interface PlatformHandshakeDecisionAdapter<E> {
	/**
	 * Processes a handshake event.
	 *
	 * @param event platform event
	 * @return completion stage that finishes when processing completes
	 */
	@NotNull CompletionStage<Void> process(@NotNull E event);
}
