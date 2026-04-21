package me.whereareiam.identica.event.routing.attempt;

import me.whereareiam.identica.event.routing.intent.RoutingIntentEvent;
import me.whereareiam.identica.type.routing.RoutingAttemptTrigger;
import org.jetbrains.annotations.NotNull;

/**
 * Routing event emitted for a concrete attempt trigger.
 */
public interface RoutingAttemptEvent extends RoutingIntentEvent {
	@NotNull RoutingAttemptTrigger getTrigger();
}
