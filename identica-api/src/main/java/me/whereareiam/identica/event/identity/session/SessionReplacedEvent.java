package me.whereareiam.identica.event.identity.session;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import me.whereareiam.identica.event.base.Event;
import me.whereareiam.identica.event.base.SynchronousEvent;
import me.whereareiam.identica.model.Session;
import org.jetbrains.annotations.NotNull;

/**
 * Event fired when an existing session is replaced by a new one.
 */
@Getter
@ToString
@RequiredArgsConstructor
public class SessionReplacedEvent implements Event, SynchronousEvent {
	private final @NotNull Session existingSession;
	private final @NotNull Session replacementSession;
}
