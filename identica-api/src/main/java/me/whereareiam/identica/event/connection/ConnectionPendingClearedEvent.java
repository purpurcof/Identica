package me.whereareiam.identica.event.connection;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import me.whereareiam.identica.event.base.Event;
import me.whereareiam.identica.event.base.SynchronousEvent;

import java.util.UUID;

@Getter
@ToString
@RequiredArgsConstructor
public class ConnectionPendingClearedEvent implements Event, SynchronousEvent {
	private final UUID connectionUniqueId;
	private final boolean hadPending;
}
