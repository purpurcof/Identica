package me.whereareiam.identica.feature.verification.model.resolution;

import lombok.*;
import me.whereareiam.identica.feature.verification.type.status.VerificationResolutionStatus;
import org.jetbrains.annotations.Nullable;

/**
 * Result returned when resolving the provider verification requirement.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class VerificationResolutionResult {
	private VerificationResolutionStatus status;
	private String challengeId;
	private String methodId;
	private boolean required;
	private boolean recoveryCodeUsed;
	private @Nullable String message;

	/**
	 * Creates a resolution result.
	 *
	 * @param status resolution status
	 * @param challengeId challenge id
	 * @param methodId method id
	 * @param required whether verification is required
	 * @param recoveryCodeUsed whether a recovery code was used
	 * @return resolution result
	 */
	public static VerificationResolutionResult of(
			VerificationResolutionStatus status,
			@Nullable String challengeId,
			@Nullable String methodId,
			boolean required,
			boolean recoveryCodeUsed
	) {
		return VerificationResolutionResult.builder()
				.status(status)
				.challengeId(challengeId)
				.methodId(methodId)
				.required(required)
				.recoveryCodeUsed(recoveryCodeUsed)
				.build();
	}
}
