package me.whereareiam.identica.event.identity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import me.whereareiam.identica.identity.actor.Identity;
import me.whereareiam.identica.event.base.Event;
import me.whereareiam.identica.event.base.SynchronousEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Event fired after an online identity is attached.
 */
@Getter
@ToString
@RequiredArgsConstructor
public class IdentityAttachedEvent implements Event, SynchronousEvent {
	private final @NotNull Identity identity;
}
