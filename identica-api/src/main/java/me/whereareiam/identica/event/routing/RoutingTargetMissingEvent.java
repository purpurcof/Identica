package me.whereareiam.identica.event.routing;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import me.whereareiam.identica.event.base.Event;
import me.whereareiam.identica.event.base.SynchronousEvent;
import me.whereareiam.identica.model.RoutingTarget;
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
public class RoutingTargetMissingEvent implements Event, SynchronousEvent {
	private final @NotNull UUID connectionUniqueId;
	private final @Nullable String username;
	private final @NotNull RoutingTarget target;
	private boolean disconnect;
	private @Nullable Component message;
}
