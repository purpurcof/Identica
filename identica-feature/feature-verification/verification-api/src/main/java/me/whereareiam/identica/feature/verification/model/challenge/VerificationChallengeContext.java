package me.whereareiam.identica.feature.verification.model.challenge;

import lombok.*;
import me.whereareiam.identica.feature.verification.state.VerificationChallengeState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Context supplied to a method challenge process.
 *
 * @param <S> challenge state type
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class VerificationChallengeContext<S extends VerificationChallengeState> {
	private @NotNull String challengeId;
	private @NotNull UUID subjectUniqueId;
	private @NotNull String methodId;
	@Builder.Default
	private @NotNull List<me.whereareiam.identica.feature.verification.model.enrollment.VerificationEnrollment> enrollments = new ArrayList<>();
	private @Nullable String providerId;
	private @Nullable String purpose;
	private boolean required;
	private @Nullable S state;
}
