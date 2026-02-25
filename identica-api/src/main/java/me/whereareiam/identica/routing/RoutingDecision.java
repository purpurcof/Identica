package me.whereareiam.identica.routing;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import me.whereareiam.identica.pipeline.journey.step.Step;
import me.whereareiam.identica.model.RoutingTarget;
import me.whereareiam.identica.model.pipeline.journey.stage.step.StepResult;
import me.whereareiam.identica.pipeline.ScenarioContext;
import me.whereareiam.identica.type.pipeline.PipelineType;
import me.whereareiam.identica.type.pipeline.journey.StageType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Routing decision built from step execution context.
 */
@Getter
@Setter
@ToString
public class RoutingDecision {
	private final @NotNull ScenarioContext context;
	private final @NotNull PipelineType pipelineType;
	private final @NotNull StageType phase;
	private final @Nullable Step step;
	private final @Nullable StepResult result;
	private @Nullable RoutingTarget target;

	/**
	 * Creates a routing decision.
	 *
	 * @param context scenario context
	 * @param pipelineType scenario pipeline type
	 * @param phase step phase
	 * @param step step instance
	 * @param result step result
	 */
	public RoutingDecision(
			@NotNull ScenarioContext context,
			@NotNull PipelineType pipelineType,
			@NotNull StageType phase,
			@Nullable Step step,
			@Nullable StepResult result
	) {
		this.context = context;
		this.pipelineType = pipelineType;
		this.phase = phase;
		this.step = step;
		this.result = result;
	}
}
