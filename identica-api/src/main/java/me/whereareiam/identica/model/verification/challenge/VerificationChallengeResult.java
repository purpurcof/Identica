package me.whereareiam.identica.model.verification.challenge;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import me.whereareiam.identica.model.verification.process.VerificationProcessDisplay;
import me.whereareiam.identica.type.verification.VerificationChallengeStatus;
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

	public static <S extends VerificationChallengeState> @NotNull VerificationChallengeResult<S> verified(@Nullable S state) {
		return VerificationChallengeResult.<S>builder()
				.status(VerificationChallengeStatus.VERIFIED)
				.state(state)
				.build();
	}
}
