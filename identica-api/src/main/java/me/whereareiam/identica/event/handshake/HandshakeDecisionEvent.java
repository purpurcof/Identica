package me.whereareiam.identica.event.handshake;

import lombok.*;
import me.whereareiam.identica.event.base.Event;
import me.whereareiam.identica.event.base.SynchronousEvent;
import me.whereareiam.identica.model.auth.HandshakeDecision;
import me.whereareiam.identica.model.auth.HandshakeRequest;

@Getter
@Setter
@ToString
@AllArgsConstructor
@RequiredArgsConstructor
public class HandshakeDecisionEvent implements Event, SynchronousEvent {
	private final HandshakeRequest request;
	private HandshakeDecision decision;
}
