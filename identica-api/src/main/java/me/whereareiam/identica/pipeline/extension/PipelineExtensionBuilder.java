package me.whereareiam.identica.pipeline.extension;

import me.whereareiam.identica.pipeline.PipelineGroup;
import me.whereareiam.identica.model.pipeline.phase.PhasePlacement;
import me.whereareiam.identica.pipeline.PipelinePhase;
import me.whereareiam.identica.model.pipeline.journey.stage.JourneyStage;
import me.whereareiam.identica.model.pipeline.journey.stage.step.JourneyStep;
import me.whereareiam.identica.pipeline.journey.step.Step;
import me.whereareiam.identica.type.pipeline.PipelineScope;
import me.whereareiam.identica.type.pipeline.PipelineType;
import me.whereareiam.identica.type.pipeline.journey.JourneyType;
import me.whereareiam.identica.type.pipeline.journey.StageType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

/**
 * Builder used by pipeline extensions to register groups, phases, stages, and steps.
 */
@SuppressWarnings("unused")
public interface PipelineExtensionBuilder {
	void registerGroup(@NotNull PipelineScope scope, @NotNull PipelineGroup<?> group);

	void registerPhase(
			@NotNull PipelineScope scope,
			@NotNull String groupId,
			@NotNull PipelinePhase<?> phase,
			@NotNull PhasePlacement placement
	);

	void registerStage(@NotNull PipelineScope scope, @NotNull JourneyStage stage);

	void registerStep(@NotNull JourneyStep step);

	default void registerStep(
			@NotNull PipelineScope scope,
			@Nullable String providerId,
			@NotNull StageType stageType,
			@NotNull PipelineType pipelineType,
			@NotNull Step step
	) {
		registerStep(JourneyStep.builder()
				.stageId(stageType.id())
				.providerId(providerId)
				.order(step.order())
				.scenarios(EnumSet.of(pipelineType))
				.step(step)
				.build());
	}

	default void registerStep(
			@NotNull PipelineScope scope,
			@Nullable String providerId,
			@NotNull StageType stageType,
			@NotNull PipelineType pipelineType,
			@NotNull JourneyType flow,
			@NotNull Step step
	) {
		registerStep(JourneyStep.builder()
				.stageId(stageType.id())
				.providerId(providerId)
				.order(step.order())
				.scenarios(EnumSet.of(pipelineType))
				.flows(EnumSet.of(flow))
				.step(step)
				.build());
	}

	default void registerStep(
			@Nullable String providerId,
			@NotNull StageType stageType,
			@NotNull Step step
	) {
		registerStep(PipelineScope.AUTHENTICATION, providerId, stageType, PipelineType.AUTHENTICATION, step);
		registerStep(PipelineScope.REGISTRATION, providerId, stageType, PipelineType.REGISTRATION, step);
	}

	default void registerStep(
			@Nullable String providerId,
			@NotNull StageType stageType,
			@NotNull JourneyType flow,
			@NotNull Step step
	) {
		registerStep(PipelineScope.AUTHENTICATION, providerId, stageType, PipelineType.AUTHENTICATION, flow, step);
		registerStep(PipelineScope.REGISTRATION, providerId, stageType, PipelineType.REGISTRATION, flow, step);
	}

}
