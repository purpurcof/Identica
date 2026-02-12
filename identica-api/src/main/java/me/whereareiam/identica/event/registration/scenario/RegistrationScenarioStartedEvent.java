package me.whereareiam.identica.event.registration.scenario;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import me.whereareiam.identica.event.base.Event;
import me.whereareiam.identica.event.base.SynchronousEvent;
import me.whereareiam.identica.model.registration.RegistrationContext;
import me.whereareiam.identica.type.pipeline.journey.JourneyType;
import org.jetbrains.annotations.NotNull;

/**
 * Event fired when a registration scenario starts.
 */
@Getter
@ToString
@RequiredArgsConstructor
public class RegistrationScenarioStartedEvent implements Event, SynchronousEvent {
	private final @NotNull RegistrationContext context;
	private final @NotNull JourneyType flow;
	private final boolean resumed;
}
