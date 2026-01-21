package me.whereareiam.identica.event.auth.attempt;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import me.whereareiam.identica.event.base.Event;
import me.whereareiam.identica.event.base.SynchronousEvent;
import me.whereareiam.identica.model.auth.AuthContext;
import me.whereareiam.identica.model.auth.StepResult;

@Getter
@ToString
@RequiredArgsConstructor
public class AuthAttemptFinishedEvent implements Event, SynchronousEvent {
	private final AuthContext context;
	private final StepResult result;
}
