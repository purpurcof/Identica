package me.whereareiam.identica.model.verification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import me.whereareiam.identica.type.verification.VerificationGateStatus;
import org.jetbrains.annotations.Nullable;

/**
 * Result returned by the provider verification gate.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class VerificationGateResult {
	private VerificationGateStatus status;
	private String challengeId;
	private String methodId;
	private boolean required;
	private boolean recoveryCodeUsed;
	private @Nullable String message;
}
