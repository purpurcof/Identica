package me.whereareiam.identica.pipeline.journey.registry;

import me.whereareiam.identica.pipeline.ScenarioContext;
import me.whereareiam.identica.model.pipeline.journey.JourneyPlan;
import me.whereareiam.identica.model.pipeline.journey.stage.JourneyStage;
import me.whereareiam.identica.model.pipeline.journey.stage.step.JourneyStep;
import me.whereareiam.identica.pipeline.journey.step.Step;
import me.whereareiam.identica.pipeline.journey.step.type.InteractiveStep;
import me.whereareiam.identica.type.pipeline.PipelineType;
import me.whereareiam.identica.type.pipeline.journey.JourneyType;
import me.whereareiam.identica.type.pipeline.journey.StageType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;

/**
 * Registry for journey stages and steps.
 */
@SuppressWarnings("unused")
public interface JourneyRegistry {
	/**
	 * Registers a journey stage.
	 *
	 * @param stage stage to register
	 */
	void registerStage(@NotNull JourneyStage stage);

	/**
	 * Unregisters a journey stage.
	 *
	 * @param stage stage to unregister
	 */
	void unregisterStage(@NotNull JourneyStage stage);

	/**
	 * Unregisters a journey stage by id.
	 *
	 * @param stageId stage identifier
	 * @return {@code true} if a stage was removed
	 */
	boolean unregisterStage(@NotNull String stageId);

	/**
	 * Registers a journey step.
	 *
	 * @param step step to register
	 */
	void registerStep(@NotNull JourneyStep step);

	/**
	 * Removes a previously registered step by name.
	 *
	 * @param stageId stage identifier
	 * @param providerId provider identifier
	 * @param pipelineType scenario pipeline type
	 * @param stepName step name
	 * @return {@code true} if a step was removed
	 */
	boolean removeStep(
			@NotNull String stageId,
			@Nullable String providerId,
			@NotNull PipelineType pipelineType,
			@NotNull String stepName
	);

	/**
	 * Resolves a journey plan for the given context and provider.
	 *
	 * @param context scenario context
	 * @param pipelineType scenario pipeline type
	 * @param flow journey flow
	 * @param providerId provider identifier
	 * @return resolved journey plan
	 */
	@NotNull JourneyPlan resolvePlan(
			@NotNull ScenarioContext context,
			@NotNull PipelineType pipelineType,
			@NotNull JourneyType flow,
			@Nullable String providerId
	);

	default @NotNull JourneyPlan resolvePlan(
			@NotNull ScenarioContext context,
			@NotNull PipelineType pipelineType,
			@NotNull JourneyType flow
	) {
		return resolvePlan(context, pipelineType, flow, null);
	}

	/**
	 * Returns all registered stages.
	 *
	 * @return registered stages
	 */
	@NotNull List<JourneyStage> getAllStages();

	/**
	 * Convenience registration for a single flow set determined by step type.
	 *
	 * @param providerId provider identifier
	 * @param stageType stage type
	 * @param order step order override
	 * @param pipelineType scenario pipeline type
	 * @param step step to register
	 */
	default void registerStep(
			@Nullable String providerId,
			@NotNull StageType stageType,
			int order,
			@NotNull PipelineType pipelineType,
			@NotNull Step step
	) {
		registerStep(JourneyStep.builder()
				.stageId(stageType.id())
				.providerId(providerId)
				.order(order)
				.scenarios(EnumSet.of(pipelineType))
				.flows(resolveDefaultFlows(step))
				.step(step)
				.build());
	}

	/**
	 * Convenience registration for a specific journey flow.
	 *
	 * @param providerId provider identifier
	 * @param stageType stage type
	 * @param order step order override
	 * @param pipelineType scenario pipeline type
	 * @param flow journey flow
	 * @param step step to register
	 */
	default void registerStep(
			@Nullable String providerId,
			@NotNull StageType stageType,
			int order,
			@NotNull PipelineType pipelineType,
			@NotNull JourneyType flow,
			@NotNull Step step
	) {
		registerStep(JourneyStep.builder()
				.stageId(stageType.id())
				.providerId(providerId)
				.order(order)
				.scenarios(EnumSet.of(pipelineType))
				.flows(EnumSet.of(flow))
				.step(step)
				.build());
	}

	private static @NotNull EnumSet<JourneyType> resolveDefaultFlows(@NotNull Step step) {
		boolean interactive = step instanceof InteractiveStep;
		if (interactive) return EnumSet.of(JourneyType.INTERACTIVE);

		return EnumSet.allOf(JourneyType.class);
	}
}
