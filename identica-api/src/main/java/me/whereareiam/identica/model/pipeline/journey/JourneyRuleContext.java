package me.whereareiam.identica.model.pipeline.journey;

import me.whereareiam.identica.pipeline.ScenarioContext;
import me.whereareiam.identica.type.pipeline.PipelineType;
import me.whereareiam.identica.type.pipeline.journey.JourneyType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record JourneyRuleContext(
		@NotNull ScenarioContext context,
		@NotNull PipelineType pipelineType,
		@NotNull JourneyType flow,
		@Nullable JourneyStateItem pending
) {
}
