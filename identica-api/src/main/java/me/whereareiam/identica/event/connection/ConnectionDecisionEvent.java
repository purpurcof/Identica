package me.whereareiam.identica.event.connection;

import lombok.*;
import me.whereareiam.identica.event.base.Event;
import me.whereareiam.identica.event.base.SynchronousEvent;
import me.whereareiam.identica.model.auth.AuthContext;
import me.whereareiam.identica.model.auth.ConnectionDecision;

@Getter
@Setter
@ToString
@AllArgsConstructor
@RequiredArgsConstructor
public class ConnectionDecisionEvent implements Event, SynchronousEvent {
	private final AuthContext context;
	private ConnectionDecision decision;
}
