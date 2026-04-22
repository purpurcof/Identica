package me.whereareiam.identica.verification;

import me.whereareiam.identica.model.verification.challenge.VerificationChallengeContext;
import me.whereareiam.identica.model.verification.challenge.VerificationChallengeResult;
import me.whereareiam.identica.model.verification.challenge.VerificationChallengeState;
import org.jetbrains.annotations.NotNull;

/**
 * Method-owned login or step-up challenge process.
 *
 * @param <S> challenge state type
 */
public interface VerificationChallengeProcess<S extends VerificationChallengeState> {
	/**
	 * Returns the persisted challenge state type.
	 *
	 * @return state class
	 */
	@NotNull Class<S> stateType();

	/**
	 * Starts a challenge.
	 *
	 * @param context challenge context
	 * @return challenge result
	 */
	@NotNull VerificationChallengeResult<S> start(@NotNull VerificationChallengeContext<S> context);

	/**
	 * Submits an interaction to a challenge.
	 *
	 * @param context challenge context
	 * @param state current challenge state
	 * @param interaction interaction to consume
	 * @return challenge result
	 */
	@NotNull VerificationChallengeResult<S> submit(
			@NotNull VerificationChallengeContext<S> context,
			@NotNull S state,
			@NotNull VerificationInteraction interaction
	);
}
