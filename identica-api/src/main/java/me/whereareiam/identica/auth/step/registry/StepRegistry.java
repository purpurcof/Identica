package me.whereareiam.identica.auth.step.registry;

import me.whereareiam.identica.auth.step.AuthenticationStep;
import me.whereareiam.identica.model.step.StepDefinition;
import me.whereareiam.identica.type.step.AuthFlowType;
import me.whereareiam.identica.type.step.StepPhase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Registry for authentication steps.
 */
public interface StepRegistry {
	/**
	 * Registers a step for both flows.
	 *
	 * @param providerId provider id or {@code null} for global steps
	 * @param phase step phase
	 * @param order execution order within the phase
	 * @param step step instance
	 */
	void register(
			@Nullable String providerId,
			@NotNull StepPhase phase,
			int order,
			@NotNull AuthenticationStep step
	);

	/**
	 * Registers a step for a specific flow.
	 *
	 * @param providerId provider id or {@code null} for global steps
	 * @param phase step phase
	 * @param order execution order within the phase
	 * @param flow target flow
	 * @param step step instance
	 */
	void register(
			@Nullable String providerId,
			@NotNull StepPhase phase,
			int order,
			@NotNull AuthFlowType flow,
			@NotNull AuthenticationStep step
	);

	/**
	 * Resolves steps for a eligibility/phase/flow.
	 *
	 * @param providerId provider id or {@code null} for global steps
	 * @param phase step phase
	 * @param flow target flow
	 * @return resolved step definitions
	 */
	@NotNull List<StepDefinition> resolve(
			@Nullable String providerId,
			@NotNull StepPhase phase,
			@NotNull AuthFlowType flow
	);

	/**
	 * Returns all registered steps.
	 *
	 * @return registered step definitions
	 */
	@NotNull List<StepDefinition> getAll();
}
