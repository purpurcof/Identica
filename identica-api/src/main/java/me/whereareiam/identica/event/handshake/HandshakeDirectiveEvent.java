package me.whereareiam.identica.event.handshake;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import me.whereareiam.identica.event.base.CancellableEvent;
import me.whereareiam.identica.event.base.Event;
import me.whereareiam.identica.event.base.SynchronousEvent;
import me.whereareiam.identica.model.auth.HandshakeDirective;
import org.jetbrains.annotations.NotNull;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
public class HandshakeDirectiveEvent implements Event, SynchronousEvent, CancellableEvent {
	@NotNull
	private HandshakeDirective directive;
	private boolean cancelled;
}
