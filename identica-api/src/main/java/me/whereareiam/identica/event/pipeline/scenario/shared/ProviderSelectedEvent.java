package me.whereareiam.identica.event.pipeline.scenario.shared;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import me.whereareiam.identica.event.base.Event;
import me.whereareiam.identica.event.base.SynchronousEvent;
import me.whereareiam.identica.pipeline.ScenarioContext;
import me.whereareiam.identica.model.provider.InternalProvider;
import me.whereareiam.identica.type.pipeline.journey.JourneyMode;
import org.jetbrains.annotations.NotNull;

/**
 * Event fired when a provider is selected for an authentication journey.
 */
@Getter
@ToString
@RequiredArgsConstructor
public class ProviderSelectedEvent implements Event, SynchronousEvent {
	private final @NotNull ScenarioContext context;
	private final @NotNull InternalProvider provider;
	private final @NotNull JourneyMode journeyMode;
}
