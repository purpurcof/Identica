package me.whereareiam.identica.event.identity.session;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import me.whereareiam.identica.event.base.CancellableEvent;
import me.whereareiam.identica.event.base.Event;
import me.whereareiam.identica.event.base.SynchronousEvent;
import me.whereareiam.identica.model.Session;
import org.jetbrains.annotations.NotNull;

/**
 * Event fired before a session is opened.
 */
@Getter
@Setter
@ToString
@RequiredArgsConstructor
public class SessionPrepareEvent implements Event, SynchronousEvent, CancellableEvent {
	private @NotNull Session session;
	private boolean cancelled;
}
