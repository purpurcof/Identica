package me.whereareiam.identica.verification.process.base;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.model.verification.process.VerificationProcessContext;
import me.whereareiam.identica.model.verification.process.VerificationProcessResult;
import me.whereareiam.identica.model.verification.process.VerificationProcessState;
import me.whereareiam.identica.model.verification.process.VerificationProcessTransition;
import me.whereareiam.identica.model.verification.process.VerificationProcessTransitionType;
import me.whereareiam.identica.verification.VerificationInteraction;
import me.whereareiam.identica.verification.process.VerificationStepCursor;
import me.whereareiam.identica.verification.process.VerificationProcessStep;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Default reusable orchestrator for step-driven verification processes.
 *
 * <p>Methods can reuse this orchestrator when their flow consists of a stable
 * entry step plus a transition graph. Methods with bespoke behavior can still
 * implement orchestration directly in their own process type.</p>
 *
 * <pre>{@code
 * VerificationProcessOrchestrator<MyState> orchestrator =
 *         new VerificationProcessOrchestrator<>(
 *                 "setup",
 *                 Map.of("setup", setupStep, "confirm", confirmStep),
 *                 Map.of("setup", "confirm")
 *         );
 * }</pre>
 *
 * @param <S> process state type
 */
@Getter
@RequiredArgsConstructor
public class VerificationProcessOrchestrator<S extends VerificationProcessState> {
	private final @NotNull String entryStepId;
	private final @NotNull Map<String, VerificationProcessStep<?, S>> stepById;
	private final @NotNull Map<String, String> nextStepById;

	/**
	 * Starts the process from the configured entry step.
	 *
	 * @param context process context
	 * @return process result after transition resolution
	 */
	public @NotNull VerificationProcessResult<S> start(@NotNull VerificationProcessContext<S> context) {
		return execute(entryStepId, context, null);
	}

	/**
	 * Resumes the process at a specific step without a new interaction.
	 *
	 * @param stepId step to execute
	 * @param context process context
	 * @return process result after transition resolution
	 */
	public @NotNull VerificationProcessResult<S> resume(
			@NotNull String stepId,
			@NotNull VerificationProcessContext<S> context
	) {
		return execute(stepId, context, null);
	}

	/**
	 * Submits an interaction to the specified step.
	 *
	 * @param stepId step to execute
	 * @param context process context
	 * @param interaction interaction to consume
	 * @return process result after transition resolution
	 */
	public @NotNull VerificationProcessResult<S> submit(
			@NotNull String stepId,
			@NotNull VerificationProcessContext<S> context,
			@NotNull VerificationInteraction interaction
	) {
		return execute(stepId, context, interaction);
	}

	private @NotNull VerificationProcessResult<S> execute(
			@NotNull String stepId,
			@NotNull VerificationProcessContext<S> context,
			@Nullable VerificationInteraction interaction
	) {
		VerificationProcessStep<?, S> rawStep = stepById.get(stepId);
		if (rawStep == null)
			throw new IllegalStateException("Unknown verification process step: " + stepId);

		VerificationProcessResult<S> result = executeStep(rawStep, context, interaction);
		return applyTransition(stepId, result);
	}

	private <I extends VerificationInteraction> @NotNull VerificationProcessResult<S> executeStep(
			@NotNull VerificationProcessStep<?, S> rawStep,
			@NotNull VerificationProcessContext<S> context,
			@Nullable VerificationInteraction interaction
	) {
		@SuppressWarnings("unchecked")
		VerificationProcessStep<I, S> step = (VerificationProcessStep<I, S>) rawStep;
		if (interaction != null && step.interactionType().isInstance(interaction))
			return step.submit(context, step.interactionType().cast(interaction));

		return step.start(context);
	}

	private @NotNull VerificationProcessResult<S> applyTransition(
			@NotNull String currentStepId,
			@NotNull VerificationProcessResult<S> result
	) {
		S state = result.getState();
		if (state == null)
			return result;

		VerificationProcessTransition transition = result.getTransition();
		if (transition == null)
			transition = VerificationProcessTransition.stay();

		applyCursor(state, resolveStepId(currentStepId, transition));
		return result.toBuilder()
				.state(state)
				.transition(transition)
				.build();
	}

	private @Nullable String resolveStepId(
			@NotNull String currentStepId,
			@NotNull VerificationProcessTransition transition
	) {
		VerificationProcessTransitionType type = transition.getType();
		if (type == VerificationProcessTransitionType.STAY)
			return currentStepId;
		if (type == VerificationProcessTransitionType.ADVANCE) {
			String nextStepId = nextStepById.get(currentStepId);
			if (nextStepId == null)
				throw new IllegalStateException("No next verification process step configured for: " + currentStepId);

			return nextStepId;
		}
		if (type == VerificationProcessTransitionType.GOTO) {
			String targetStepId = transition.getTargetStepId();
			if (targetStepId == null || !stepById.containsKey(targetStepId))
				throw new IllegalStateException("Unknown verification process transition target: " + targetStepId);

			return targetStepId;
		}
		if (type == VerificationProcessTransitionType.COMPLETE)
			return null;

		throw new IllegalStateException("Unsupported verification process transition: " + type);
	}

	private void applyCursor(@NotNull S state, @Nullable String stepId) {
		if (stepId == null)
			return;
		if (state instanceof VerificationStepCursor cursor)
			cursor.setStepId(stepId);
	}
}
