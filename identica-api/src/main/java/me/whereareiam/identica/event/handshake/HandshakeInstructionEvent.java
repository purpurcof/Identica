package me.whereareiam.identica.event.handshake;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import me.whereareiam.identica.event.base.CancellableEvent;
import me.whereareiam.identica.event.base.Event;
import me.whereareiam.identica.event.base.SynchronousEvent;
import me.whereareiam.identica.model.auth.handshake.HandshakeInstruction;
import org.jetbrains.annotations.NotNull;

/**
 * Event fired when a handshake instruction is stored.
 */
@Getter
@Setter
@ToString
@RequiredArgsConstructor
public class HandshakeInstructionEvent implements Event, SynchronousEvent, CancellableEvent {
	private final @NotNull HandshakeInstruction instruction;
	private boolean cancelled;
}
