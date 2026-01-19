package me.whereareiam.identica.event.base;

import me.whereareiam.identica.type.event.EventOrder;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as an Identica event handler.
 * Methods annotated with this annotation will be registered as event listeners
 * in the Identica event system.
 *
 * <p>Event handlers can specify their execution order using the {@link EventOrder}
 * parameter. By default, events use {@link EventOrder#NORMAL} priority.</p>
 * <p>
 * Example usage:
 * <pre>
 * &#64;IdenticEvent(EventOrder.HIGH)
 * public void onPlayerChat(ChatEvent event) {
 *     // Handle chat event
 * }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface IdenticEvent {
	/**
	 * The order in which this event handler should be executed
	 */
	EventOrder value() default EventOrder.NORMAL;
}
