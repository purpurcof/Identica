package me.whereareiam.identica.model.routing;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import me.whereareiam.identica.model.pipeline.PipelineResult;
import me.whereareiam.identica.model.pipeline.journey.stage.step.StepResult;
import me.whereareiam.identica.pipeline.ScenarioContext;
import me.whereareiam.identica.pipeline.journey.step.Step;
import me.whereareiam.identica.type.pipeline.PipelineType;
import me.whereareiam.identica.type.pipeline.journey.StageType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Pipeline fact submitted to the routing coordinator.
 */
@Getter
@ToString
@RequiredArgsConstructor
public class RoutingSignal {
	private final @NotNull RoutingSignalType type;
	private final @NotNull ScenarioContext context;
	private final @NotNull PipelineType pipelineType;
	private final @Nullable StageType stage;
	private final @Nullable Step step;
	private final @Nullable StepResult stepResult;
	private final @Nullable PipelineResult pipelineResult;

	public static @NotNull RoutingSignal stepFinished(
			@NotNull ScenarioContext context,
			@NotNull PipelineType pipelineType,
			@NotNull StageType stage,
			@NotNull Step step,
			@NotNull StepResult result
	) {
		return new RoutingSignal(RoutingSignalType.STEP_FINISHED, context, pipelineType, stage, step, result, null);
	}

	public static @NotNull RoutingSignal pipelineFinished(
			@NotNull ScenarioContext context,
			@NotNull PipelineType pipelineType,
			@NotNull PipelineResult result
	) {
		return new RoutingSignal(RoutingSignalType.PIPELINE_FINISHED, context, pipelineType, null, null, null, result);
	}

	public @Nullable UUID connectionUniqueId() {
		return context.getConnectionUniqueId();
	}

	public enum RoutingSignalType {
		STEP_FINISHED,
		PIPELINE_FINISHED
	}
}
