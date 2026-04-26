package me.whereareiam.identica.verification.process;

import me.whereareiam.identica.model.verification.enrollment.VerificationEnrollmentContext;
import me.whereareiam.identica.model.verification.enrollment.VerificationEnrollmentResult;
import me.whereareiam.identica.model.verification.enrollment.VerificationEnrollmentState;
import me.whereareiam.identica.verification.VerificationInteraction;
import org.jetbrains.annotations.NotNull;

/**
 * Method-owned enrollment process.
 *
 * @param <S> enrollment state type
 */
public interface VerificationEnrollmentProcess<S extends VerificationEnrollmentState> {
	/**
	 * Returns the persisted enrollment state type.
	 *
	 * @return state class
	 */
	@NotNull Class<S> stateType();

	/**
	 * Starts enrollment.
	 *
	 * @param context enrollment context
	 * @return enrollment result
	 */
	@NotNull VerificationEnrollmentResult<S> start(@NotNull VerificationEnrollmentContext<S> context);

	/**
	 * Submits an interaction to the enrollment process.
	 *
	 * @param context enrollment context
	 * @param state current enrollment state
	 * @param interaction interaction to consume
	 * @return enrollment result
	 */
	@NotNull VerificationEnrollmentResult<S> submit(
			@NotNull VerificationEnrollmentContext<S> context,
			@NotNull S state,
			@NotNull VerificationInteraction interaction
	);
}
