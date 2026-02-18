package me.whereareiam.identica.event.pipeline.scenario.authentication;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import me.whereareiam.identica.event.base.Event;
import me.whereareiam.identica.event.base.SynchronousEvent;
import me.whereareiam.identica.model.auth.AuthContext;

@Getter
@ToString
@RequiredArgsConstructor
public class AuthenticationContextBuiltEvent implements Event, SynchronousEvent {
	private final AuthContext context;
}
