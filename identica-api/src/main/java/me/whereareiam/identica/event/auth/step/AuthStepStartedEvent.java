package me.whereareiam.identica.event.auth.step;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import me.whereareiam.identica.auth.step.AuthenticationStep;
import me.whereareiam.identica.event.base.Event;
import me.whereareiam.identica.event.base.SynchronousEvent;
import me.whereareiam.identica.loader.IdenticaProvider;
import me.whereareiam.identica.model.auth.AuthContext;

@Getter
@ToString
@RequiredArgsConstructor
public class AuthStepStartedEvent implements Event, SynchronousEvent {
	private final IdenticaProvider provider;
	private final AuthenticationStep step;
	private final AuthContext context;
}
