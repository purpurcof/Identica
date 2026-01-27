package me.whereareiam.identica.util;

import com.google.inject.Singleton;
import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.event.base.CancellableEvent;
import me.whereareiam.identica.event.base.Event;

/**
 * Utility helper for dispatching Identica events with optional cancellation handling.
 */
@Singleton
@SuppressWarnings({"unused", "UnusedReturnValue"})
public class EventUtil {
	private static EventManager eventManager;

	/**
	 * Initialize the EventUtil with the active event manager.
	 *
	 * @param eventManager event manager used for dispatching events
	 */
	public static void initialize(EventManager eventManager) {
		EventUtil.eventManager = eventManager;
	}

	/**
	 * Dispatches an event and runs the callback if the event is not cancelled.
	 *
	 * @param event event to dispatch
	 * @param callback callback executed when event is not cancelled
	 * @return true if callback executed, false when event was cancelled
	 */
	public static boolean callEvent(Event event, Runnable callback) {
		eventManager.call(event);
		if (event instanceof CancellableEvent cancellableEvent
				&& cancellableEvent.isCancelled())
			return false;

		callback.run();
		return true;
	}

	/**
	 * Dispatches an event.
	 *
	 * @param event event to dispatch
	 * @return true if event was not cancelled
	 */
	public static boolean callEvent(Event event) {
		return callEvent(event, () -> {
		});
	}
}
