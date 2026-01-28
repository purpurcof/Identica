package me.whereareiam.identica.event.identity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import me.whereareiam.identica.event.base.Event;
import me.whereareiam.identica.event.base.SynchronousEvent;
import me.whereareiam.identica.model.identity.IdentityState;
import org.jetbrains.annotations.NotNull;

/**
 * Event fired after an identity state enters the pending phase.
 */
@Getter
@ToString
@RequiredArgsConstructor
public class IdentityPendingEvent implements Event, SynchronousEvent {
	private final @NotNull IdentityState state;
}
