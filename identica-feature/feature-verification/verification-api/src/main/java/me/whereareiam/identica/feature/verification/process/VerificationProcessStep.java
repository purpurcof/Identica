package me.whereareiam.identica.feature.verification.process;

import me.whereareiam.identica.feature.verification.VerificationInteraction;
import me.whereareiam.identica.feature.verification.model.process.VerificationProcessContext;
import me.whereareiam.identica.feature.verification.model.process.VerificationProcessResult;
import me.whereareiam.identica.feature.verification.state.VerificationProcessState;
import org.jetbrains.annotations.NotNull;

/**
 * Typed step inside a verification process.
 *
 * @param <I> accepted interaction type
 * @param <S> process state type
 */
public interface VerificationProcessStep<I extends VerificationInteraction, S extends VerificationProcessState> {
	/**
	 * Returns the stable step id.
	 *
	 * @return step id
	 */
	@NotNull String id();

	/**
	 * Returns the interaction type accepted by this step.
	 *
	 * @return interaction class
	 */
	@NotNull Class<I> interactionType();

	/**
	 * Returns the state type used by this step.
	 *
	 * @return state class
	 */
	@NotNull Class<S> stateType();

	/**
	 * Starts or resumes the step without a new interaction.
	 *
	 * @param context typed process context
	 * @return process result
	 */
	@NotNull VerificationProcessResult<S> start(@NotNull VerificationProcessContext<S> context);

	/**
	 * Submits an interaction to the step.
	 *
	 * @param context typed process context
	 * @param interaction interaction to consume
	 * @return process result
	 */
	@NotNull VerificationProcessResult<S> submit(
			@NotNull VerificationProcessContext<S> context,
			@NotNull I interaction
	);
}
