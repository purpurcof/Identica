package me.whereareiam.identica.event.routing.completion;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import me.whereareiam.identica.event.base.SynchronousEvent;
import me.whereareiam.identica.event.routing.intent.RoutingIntentEvent;
import me.whereareiam.identica.model.routing.RoutingIntent;

@Getter
@ToString
@RequiredArgsConstructor
public class CompletionRoutingReachedEvent implements RoutingIntentEvent, SynchronousEvent {
	private final RoutingIntent intent;
	private final String currentServer;
}
