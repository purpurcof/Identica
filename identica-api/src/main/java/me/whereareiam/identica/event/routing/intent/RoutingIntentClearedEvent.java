package me.whereareiam.identica.event.routing.intent;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import me.whereareiam.identica.event.base.SynchronousEvent;
import me.whereareiam.identica.event.routing.RoutingEvent;
import me.whereareiam.identica.model.routing.RoutingIntent;
import me.whereareiam.identica.type.routing.reason.RoutingClearReason;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@Getter
@ToString
@RequiredArgsConstructor
public class RoutingIntentClearedEvent implements RoutingEvent, SynchronousEvent {
	private final UUID connectionUniqueId;
	private final @Nullable RoutingIntent intent;
	private final RoutingClearReason reason;
}
