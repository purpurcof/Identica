package me.whereareiam.identica.event.pipeline.attempt;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import me.whereareiam.identica.event.base.Event;
import me.whereareiam.identica.event.base.SynchronousEvent;
import me.whereareiam.identica.pipeline.ScenarioContext;

/**
 * Event fired when a scenario context is built for a pipeline attempt.
 */
@Getter
@ToString
@RequiredArgsConstructor
public class ScenarioContextBuiltEvent implements Event, SynchronousEvent {
	private final ScenarioContext context;
}
