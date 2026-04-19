package me.whereareiam.identica.model.verification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import me.whereareiam.identica.type.verification.status.VerificationAttemptStatus;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class VerificationAttemptResult {
	private VerificationAttemptStatus status;
	private String methodId;
	private boolean required;
	private boolean recoveryCodeUsed;
}
