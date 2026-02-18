package me.whereareiam.identica.event.pipeline.scenario.registration;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import me.whereareiam.identica.event.base.Event;
import me.whereareiam.identica.event.base.SynchronousEvent;
import me.whereareiam.identica.model.registration.RegistrationContext;

@Getter
@ToString
@RequiredArgsConstructor
public class RegistrationContextBuiltEvent implements Event, SynchronousEvent {
	private final RegistrationContext context;
}
