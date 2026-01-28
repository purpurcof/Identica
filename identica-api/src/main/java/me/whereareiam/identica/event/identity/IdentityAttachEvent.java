package me.whereareiam.identica.event.identity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import me.whereareiam.identica.identity.actor.Identity;
import me.whereareiam.identica.event.base.CancellableEvent;
import me.whereareiam.identica.event.base.Event;
import me.whereareiam.identica.event.base.SynchronousEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Event fired before an online identity is attached.
 */
@Getter
@Setter
@ToString
@RequiredArgsConstructor
public class IdentityAttachEvent implements Event, SynchronousEvent, CancellableEvent {
	private @NotNull Identity identity;
	private boolean cancelled;
}
