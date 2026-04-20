package me.whereareiam.identica.event.routing;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import me.whereareiam.identica.event.base.Event;
import me.whereareiam.identica.event.base.SynchronousEvent;
import me.whereareiam.identica.model.RoutingTarget;

import java.util.UUID;

@Getter
@ToString
@RequiredArgsConstructor
public class RoutingTargetReachedEvent implements Event, SynchronousEvent {
	private final UUID connectionUniqueId;
	private final RoutingTarget target;
	private final String currentServer;
}
