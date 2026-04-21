package me.whereareiam.identica.replication.event;

import me.whereareiam.identica.event.base.ReplicatedEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Registry of event ids that are allowed to cross the replication channel.
 */
public interface ReplicatedEventRegistry {
	/**
	 * Registers a replicated event type.
	 *
	 * @param type stable wire type
	 * @param eventClass event class
	 * @param <T> event type
	 */
	<T extends ReplicatedEvent> void register(@NotNull String type, @NotNull Class<T> eventClass);

	/**
	 * Finds the wire type for an event class.
	 *
	 * @param eventClass event class
	 * @return registered wire type
	 */
	@NotNull Optional<String> typeOf(@NotNull Class<? extends ReplicatedEvent> eventClass);

	/**
	 * Finds the event class for a wire type.
	 *
	 * @param type stable wire type
	 * @return registered event class
	 */
	@NotNull Optional<Class<? extends ReplicatedEvent>> classOf(@NotNull String type);
}
