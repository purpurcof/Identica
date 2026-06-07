package me.whereareiam.identica.feature.verification.model.challenge;

import lombok.*;
import me.whereareiam.identica.feature.verification.model.process.VerificationProcessDisplay;
import me.whereareiam.identica.feature.verification.state.VerificationChallengeState;
import me.whereareiam.identica.feature.verification.type.status.VerificationChallengeStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Result returned by a challenge process.
 *
 * @param <S> challenge state type
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class VerificationChallengeResult<S extends VerificationChallengeState> {
	private VerificationChallengeStatus status;
	private String challengeId;
	private String methodId;
	private String providerId;
	private boolean required;
	private boolean recoveryCodeUsed;
	private @Nullable S state;
	private @Nullable VerificationProcessDisplay display;

	/**
	 * Creates a challenge result.
	 *
	 * @param status challenge status
	 * @param challengeId challenge id
	 * @param methodId method id
	 * @param providerId provider id
	 * @param required whether the challenge is required
	 * @param <S> challenge state type
	 * @return challenge result
	 */
	public static <S extends VerificationChallengeState> @NotNull VerificationChallengeResult<S> of(
			@NotNull VerificationChallengeStatus status,
			@Nullable String challengeId,
			@Nullable String methodId,
			@Nullable String providerId,
			boolean required
	) {
		return VerificationChallengeResult.<S>builder()
				.status(status)
				.challengeId(challengeId)
				.methodId(methodId)
				.providerId(providerId)
				.required(required)
				.recoveryCodeUsed(false)
				.state(null)
				.build();
	}

	/**
	 * Creates a waiting challenge result.
	 *
	 * @param state challenge state
	 * @param display challenge display payload
	 * @param <S> challenge state type
	 * @return waiting challenge result
	 */
	public static <S extends VerificationChallengeState> @NotNull VerificationChallengeResult<S> waiting(
			@Nullable S state,
			@Nullable VerificationProcessDisplay display
	) {
		return VerificationChallengeResult.<S>builder()
				.status(VerificationChallengeStatus.WAITING)
				.state(state)
					.display(display)
					.build();
	}

	/**
	 * Creates a verified challenge result.
	 *
	 * @param state challenge state
	 * @param <S> challenge state type
	 * @return verified challenge result
	 */
	public static <S extends VerificationChallengeState> @NotNull VerificationChallengeResult<S> verified(@Nullable S state) {
		return VerificationChallengeResult.<S>builder()
				.status(VerificationChallengeStatus.VERIFIED)
				.state(state)
				.build();
	}
}
