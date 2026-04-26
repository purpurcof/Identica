package me.whereareiam.identica.model.verification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import me.whereareiam.identica.type.verification.VerificationResolutionStatus;
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
