package me.whereareiam.identica.engine.pipeline.scenario.shared.group.journey;

import lombok.Getter;
import lombok.Setter;
import me.whereareiam.identica.engine.pipeline.scenario.shared.group.AbstractGroupState;
import me.whereareiam.identica.pipeline.ScenarioContext;
import me.whereareiam.identica.model.pipeline.journey.JourneyStateItem;
import me.whereareiam.identica.model.pipeline.journey.execution.JourneyExecutionPlan;
import me.whereareiam.identica.type.pipeline.journey.JourneyType;
import org.jetbrains.annotations.Nullable;

@Getter
@Setter
public class JourneyState extends AbstractGroupState {
	private @Nullable ScenarioContext context;
	private @Nullable JourneyType flow;
	private @Nullable JourneyStateItem pending;
	private @Nullable JourneyExecutionPlan executionPlan;
}
