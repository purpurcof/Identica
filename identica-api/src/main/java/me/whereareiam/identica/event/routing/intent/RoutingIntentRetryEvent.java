package me.whereareiam.identica.event.routing.intent;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import me.whereareiam.identica.event.base.SynchronousEvent;
import me.whereareiam.identica.model.routing.RoutingIntent;

/**
 * Requests another routing attempt for a still-pending intent.
 */
@Getter
@ToString
@RequiredArgsConstructor
public class RoutingIntentRetryEvent implements RoutingIntentEvent, SynchronousEvent {
	private final RoutingIntent intent;
}
