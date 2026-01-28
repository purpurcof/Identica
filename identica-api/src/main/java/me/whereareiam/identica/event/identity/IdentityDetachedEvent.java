package me.whereareiam.identica.event.identity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import me.whereareiam.identica.event.base.Event;
import me.whereareiam.identica.event.base.SynchronousEvent;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Event fired after an online identity is detached.
 */
@Getter
@ToString
@RequiredArgsConstructor
public class IdentityDetachedEvent implements Event, SynchronousEvent {
	private final @NotNull UUID uniqueId;
}
