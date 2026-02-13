package me.whereareiam.identica.model.pipeline.journey.stage.step;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import me.whereareiam.identica.pipeline.journey.step.Step;
import me.whereareiam.identica.type.pipeline.PipelineType;
import me.whereareiam.identica.type.pipeline.journey.JourneyType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.Set;

@Getter
@ToString
@Builder(toBuilder = true)
public class JourneyStep {
	private final @NotNull String stageId;
	private final @NotNull Step step;
	private final int order;
	private final @NotNull Set<PipelineType> scenarios;
	private final @NotNull Set<JourneyType> flows;
	private final @Nullable String providerId;

	public boolean supports(
			@NotNull PipelineType pipelineType,
			@NotNull JourneyType flow,
			@Nullable String targetProviderId
	) {
		Set<PipelineType> supportedScenarios = scenarios.isEmpty()
				? EnumSet.allOf(PipelineType.class)
				: scenarios;
		if (!supportedScenarios.contains(pipelineType))
			return false;

		Set<JourneyType> supportedFlows = flows.isEmpty()
				? EnumSet.allOf(JourneyType.class)
				: flows;
		if (!supportedFlows.contains(flow))
			return false;

		if (providerId == null || providerId.isBlank())
			return true;
		return providerId.equalsIgnoreCase(targetProviderId);
	}

	public int resolvedOrder() {
		if (order != 0)
			return order;
		return step.order();
	}

	public boolean isFlowSpecific(@NotNull JourneyType flow) {
		return flows.size() == 1 && flows.contains(flow);
	}

	public boolean isProviderSpecific() {
		return providerId != null && !providerId.isBlank();
	}
}
