package me.whereareiam.identica.event.auth;

import lombok.*;
import me.whereareiam.identica.event.base.Event;
import me.whereareiam.identica.event.base.SynchronousEvent;
import me.whereareiam.identica.model.auth.AuthContext;
import me.whereareiam.identica.model.auth.AuthDecision;

@Getter
@Setter
@ToString
@AllArgsConstructor
@RequiredArgsConstructor
public class AuthDecisionEvent implements Event, SynchronousEvent {
	private final AuthContext context;
	private AuthDecision decision;
}
