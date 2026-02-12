package me.whereareiam.identica.event.registration;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import me.whereareiam.identica.event.base.Event;
import me.whereareiam.identica.event.base.SynchronousEvent;
import me.whereareiam.identica.model.registration.RegistrationContext;

@Getter
@ToString
@RequiredArgsConstructor
public class RegistrationContextBuildEvent implements Event, SynchronousEvent {
	private final RegistrationContext context;
}
