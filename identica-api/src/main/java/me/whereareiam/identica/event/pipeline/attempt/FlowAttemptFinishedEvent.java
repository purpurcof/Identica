package me.whereareiam.identica.event.pipeline.attempt;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import me.whereareiam.identica.event.base.Event;
import me.whereareiam.identica.event.base.SynchronousEvent;
import me.whereareiam.identica.pipeline.ScenarioContext;
import me.whereareiam.identica.model.pipeline.PipelineResult;

@Getter
@ToString
@RequiredArgsConstructor
public class FlowAttemptFinishedEvent implements Event, SynchronousEvent {
	private final ScenarioContext context;
	private final PipelineResult result;
}
