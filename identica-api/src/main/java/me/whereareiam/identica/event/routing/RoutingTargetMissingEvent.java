package me.whereareiam.identica.event.routing;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import me.whereareiam.identica.event.base.SynchronousEvent;
import me.whereareiam.identica.event.routing.intent.RoutingIntentEvent;
import me.whereareiam.identica.model.routing.RoutingIntent;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Event fired when a routing target server is missing during connection handling.
 */
@Getter
@Setter
@ToString
@RequiredArgsConstructor
public class RoutingTargetMissingEvent implements RoutingIntentEvent, SynchronousEvent {
	private final @NotNull UUID connectionUniqueId;
	private final @Nullable String username;
	private final @NotNull RoutingIntent intent;
	private boolean disconnect;
	private @Nullable Component message;
}
