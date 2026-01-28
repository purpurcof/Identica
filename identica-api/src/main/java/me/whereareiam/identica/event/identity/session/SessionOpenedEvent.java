package me.whereareiam.identica.event.identity.session;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import me.whereareiam.identica.event.base.Event;
import me.whereareiam.identica.event.base.SynchronousEvent;
import me.whereareiam.identica.model.Session;
import org.jetbrains.annotations.NotNull;

/**
 * Event fired after a session is opened.
 */
@Getter
@ToString
@RequiredArgsConstructor
public class SessionOpenedEvent implements Event, SynchronousEvent {
	private final @NotNull Session session;
}
