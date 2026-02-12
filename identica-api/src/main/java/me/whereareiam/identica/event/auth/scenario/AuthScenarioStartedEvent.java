package me.whereareiam.identica.event.auth.scenario;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import me.whereareiam.identica.event.base.Event;
import me.whereareiam.identica.event.base.SynchronousEvent;
import me.whereareiam.identica.model.auth.AuthContext;
import me.whereareiam.identica.type.pipeline.journey.JourneyType;
import org.jetbrains.annotations.NotNull;

/**
 * Event fired when an authentication scenario starts.
 */
@Getter
@ToString
@RequiredArgsConstructor
public class AuthScenarioStartedEvent implements Event, SynchronousEvent {
	private final @NotNull AuthContext context;
	private final @NotNull JourneyType flow;
	private final boolean resumed;
}
