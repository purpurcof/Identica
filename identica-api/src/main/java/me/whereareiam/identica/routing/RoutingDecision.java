package me.whereareiam.identica.routing;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import me.whereareiam.identica.auth.step.AuthenticationStep;
import me.whereareiam.identica.model.RoutingTarget;
import me.whereareiam.identica.model.auth.AuthContext;
import me.whereareiam.identica.model.auth.StepResult;
import me.whereareiam.identica.type.step.StepPhase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Routing decision built from step execution context.
 */
@Getter
@Setter
@ToString
public class RoutingDecision {
	private final @NotNull AuthContext context;
	private final @NotNull StepPhase phase;
	private final @Nullable AuthenticationStep step;
	private final @Nullable StepResult result;
	private @Nullable RoutingTarget target;

	/**
	 * Creates a routing decision.
	 *
	 * @param context authentication context
	 * @param phase step phase
	 * @param step step instance
	 * @param result step result
	 */
	public RoutingDecision(
			@NotNull AuthContext context,
			@NotNull StepPhase phase,
			@Nullable AuthenticationStep step,
			@Nullable StepResult result
	) {
		this.context = context;
		this.phase = phase;
		this.step = step;
		this.result = result;
	}
}
