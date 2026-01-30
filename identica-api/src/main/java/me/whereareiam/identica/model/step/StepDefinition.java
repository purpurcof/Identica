package me.whereareiam.identica.model.step;

import lombok.Getter;
import lombok.ToString;
import me.whereareiam.identica.auth.step.AuthenticationStep;
import me.whereareiam.identica.type.step.AuthFlowType;
import me.whereareiam.identica.type.step.StepPhase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Definition of a step registered in the authentication flow.
 */
@Getter
@ToString
public class StepDefinition {
	private final @Nullable String providerId;
	private final @NotNull StepPhase phase;
	private final int order;
	private final @Nullable AuthFlowType flow;
	private final @NotNull AuthenticationStep step;
	private final @NotNull String name;

	/**
	 * Creates a step definition.
	 *
	 * @param providerId provider id or {@code null} for global steps
	 * @param phase step phase
	 * @param order execution order within the phase
	 * @param flow target flow or {@code null} for both flows
	 * @param step step instance
	 */
	public StepDefinition(
			@Nullable String providerId,
			@NotNull StepPhase phase,
			int order,
			@Nullable AuthFlowType flow,
			@NotNull AuthenticationStep step
	) {
		this.providerId = providerId;
		this.phase = Objects.requireNonNull(phase, "phase");
		this.order = order;
		this.flow = flow;
		this.step = Objects.requireNonNull(step, "step");
		this.name = Objects.requireNonNull(step.getName(), "name");
	}
}
