package me.whereareiam.identica.auth.stage;

import me.whereareiam.identica.auth.step.AuthenticationStep;
import me.whereareiam.identica.model.auth.AuthContext;
import me.whereareiam.identica.model.provider.InternalProvider;
import me.whereareiam.identica.type.step.AuthFlowType;
import me.whereareiam.identica.type.step.StepPhase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Represents a stage in the authentication pipeline.
 */
public interface StepStage {
	/**
	 * Unique stage identifier.
	 *
	 * @return stage id
	 */
	@NotNull String id();

	/**
	 * Stage execution order (lower runs first).
	 *
	 * @return stage order
	 */
	int order();

	/**
	 * Stage phase used for routing and event context.
	 *
	 * @return stage phase
	 */
	@NotNull StepPhase phase();

	/**
	 * Whether this stage runs eligibility-specific steps.
	 *
	 * @return {@code true} if eligibility-scoped
	 */
	boolean providerStage();

	/**
	 * Determines whether the stage should run for the given context and flow.
	 *
	 * @param context authentication context
	 * @param flow flow type
	 * @return {@code true} if the stage should run
	 */
	boolean supports(
			@NotNull AuthContext context,
			@NotNull AuthFlowType flow
	);

	/**
	 * Returns steps to execute for the stage.
	 *
	 * @param context authentication context
	 * @param flow flow type
	 * @param provider provider or {@code null} for global stages
	 * @return steps to execute
	 */
	@NotNull List<AuthenticationStep> steps(
			@NotNull AuthContext context,
			@NotNull AuthFlowType flow,
			@Nullable InternalProvider provider
	);

	/**
	 * Determines whether a eligibility-stage should fallback to another eligibility.
	 *
	 * @param context authentication context
	 * @param flow flow type
	 * @return {@code true} if fallback is allowed
	 */
	boolean allowFallback(
			@NotNull AuthContext context,
			@NotNull AuthFlowType flow
	);

	/**
	 * Whether the stage requires a completion step (COMPLETE).
	 *
	 * @return {@code true} if completion is required
	 */
	boolean requireCompletion();

	/**
	 * Whether to return the completion result when the stage completes.
	 *
	 * @return {@code true} if completion result should be returned
	 */
	boolean usesCompletionResult();
}
