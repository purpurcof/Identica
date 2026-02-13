package me.whereareiam.identica.model.pipeline.journey.stage;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import me.whereareiam.identica.type.pipeline.PipelineType;
import me.whereareiam.identica.type.pipeline.journey.JourneyType;
import me.whereareiam.identica.type.pipeline.journey.StageType;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.Set;

@Getter
@ToString
@Builder(toBuilder = true)
public class JourneyStage {
	private final @NotNull String id;
	private final @NotNull StageType type;
	private final int order;
	private final @NotNull Set<PipelineType> pipelineTypes;
	private final @NotNull Set<JourneyType> flows;
	private final boolean requireCompletion;
	private final boolean usesCompletionResult;
	private final boolean allowFallback;

	public boolean supports(@NotNull PipelineType pipelineType, @NotNull JourneyType flow) {
		Set<PipelineType> supportedPipelines = pipelineTypes.isEmpty()
				? EnumSet.allOf(PipelineType.class)
				: pipelineTypes;
		if (!supportedPipelines.contains(pipelineType))
			return false;

		Set<JourneyType> supportedFlows = flows.isEmpty()
				? EnumSet.allOf(JourneyType.class)
				: flows;

		return supportedFlows.contains(flow);
	}

	public boolean providerStage() {
		return StageType.PROVIDER.equals(type);
	}
}
