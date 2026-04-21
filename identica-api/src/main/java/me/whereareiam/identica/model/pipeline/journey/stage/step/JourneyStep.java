package me.whereareiam.identica.model.pipeline.journey.stage.step;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import me.whereareiam.identica.pipeline.journey.step.Step;
import me.whereareiam.identica.type.pipeline.PipelineType;
import me.whereareiam.identica.type.pipeline.journey.JourneyMode;
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
	private final @NotNull Set<JourneyMode> journeyModes;
	private final @Nullable String providerId;

	public boolean supports(
			@NotNull PipelineType pipelineType,
			@NotNull JourneyMode journeyMode,
			@Nullable String targetProviderId
	) {
		Set<PipelineType> supportedScenarios = scenarios.isEmpty()
				? EnumSet.allOf(PipelineType.class)
				: scenarios;
		if (!supportedScenarios.contains(pipelineType)) return false;

		Set<JourneyMode> supportedJourneyModes = journeyModes.isEmpty()
				? EnumSet.allOf(JourneyMode.class)
				: journeyModes;
		if (!supportedJourneyModes.contains(journeyMode)) return false;
		if (providerId == null || providerId.isBlank()) return true;

		return providerId.equalsIgnoreCase(targetProviderId);
	}

	public int resolvedOrder() {
		if (order != 0) return order;
		return step.order();
	}

	public boolean isJourneyModeSpecific(@NotNull JourneyMode journeyMode) {
        return journeyModes.size() == 1 && journeyModes.contains(journeyMode);
	}

	public boolean isProviderSpecific() {
		return providerId != null && !providerId.isBlank();
	}
}
