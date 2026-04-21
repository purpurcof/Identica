package me.whereareiam.identica.event.routing.intent;

import me.whereareiam.identica.event.routing.RoutingEvent;
import me.whereareiam.identica.model.routing.RoutingIntent;
import org.jetbrains.annotations.NotNull;

/**
 * Routing event that always carries a routing intent.
 */
public interface RoutingIntentEvent extends RoutingEvent {
	@NotNull RoutingIntent getIntent();
}
