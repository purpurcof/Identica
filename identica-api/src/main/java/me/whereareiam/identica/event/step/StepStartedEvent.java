package me.whereareiam.identica.event.step;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import me.whereareiam.identica.pipeline.journey.step.Step;
import me.whereareiam.identica.event.base.Event;
import me.whereareiam.identica.event.base.SynchronousEvent;
import me.whereareiam.identica.provider.IdenticaProvider;
import me.whereareiam.identica.pipeline.ScenarioContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Event fired after a step execution has been initiated.
 */
@Getter
@ToString
@RequiredArgsConstructor
public class StepStartedEvent implements Event, SynchronousEvent {
	private final @Nullable IdenticaProvider provider;
	private final @NotNull Step step;
	private final @NotNull ScenarioContext context;
}
