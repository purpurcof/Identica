package me.whereareiam.identica.event.identity.session;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import me.whereareiam.identica.event.base.Event;
import me.whereareiam.identica.event.base.SynchronousEvent;
import me.whereareiam.identica.model.Session;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Event fired after a session is closed.
 */
@Getter
@ToString
@RequiredArgsConstructor
public class SessionClosedEvent implements Event, SynchronousEvent {
	private final @NotNull UUID uniqueId;
	private final @Nullable Session session;
}
