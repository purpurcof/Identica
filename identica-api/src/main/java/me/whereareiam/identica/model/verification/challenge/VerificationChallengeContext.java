package me.whereareiam.identica.model.verification.challenge;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
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
	private @NotNull List<me.whereareiam.identica.model.verification.enrollment.VerificationEnrollment> enrollments = new ArrayList<>();
	private @Nullable String providerId;
	private @Nullable String purpose;
	private boolean required;
	private @Nullable S state;
}
