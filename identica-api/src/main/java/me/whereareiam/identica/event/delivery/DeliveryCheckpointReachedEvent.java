package me.whereareiam.identica.event.delivery;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import me.whereareiam.identica.event.base.Event;
import me.whereareiam.identica.event.base.SynchronousEvent;
import me.whereareiam.identica.identity.actor.Identity;
import me.whereareiam.identica.type.messaging.DeliveryCheckpoint;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Event fired when a messaging checkpoint becomes reachable for an attached identity.
 */
@Getter
@ToString
@RequiredArgsConstructor
public class DeliveryCheckpointReachedEvent implements Event, SynchronousEvent {
	private final @NotNull DeliveryCheckpoint checkpoint;
	private final @NotNull Identity identity;
	private final @Nullable String currentServer;
}
