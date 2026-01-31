package me.whereareiam.identica.listener;

import org.jetbrains.annotations.NotNull;

/**
 * Registry for dynamic listeners provided by core and providers.
 */
public interface DynamicListenerRegistry {
	/**
	 * Registers a listener for the provided event class.
	 *
	 * @param eventClass event class
	 * @param listener listener instance
	 * @param <T> event type
	 */
	<T> void register(@NotNull Class<T> eventClass, @NotNull DynamicListener<T> listener);

	/**
	 * Registers a listener by resolving its event type from the {@code onEvent} method signature.
	 *
	 * @param listener listener instance
	 */
	void register(@NotNull DynamicListener<?> listener);

	/**
	 * Attaches the platform listener registrar so late registrations can be wired immediately.
	 *
	 * @param registrar listener registrar
	 */
	void attachRegistrar(@NotNull ListenerRegistrar registrar);
}
