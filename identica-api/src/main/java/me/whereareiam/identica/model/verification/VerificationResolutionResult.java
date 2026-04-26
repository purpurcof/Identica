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
}
