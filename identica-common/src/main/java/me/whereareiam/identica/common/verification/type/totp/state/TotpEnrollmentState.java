package me.whereareiam.identica.common.verification.type.totp.state;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import me.whereareiam.identica.model.verification.enrollment.VerificationEnrollmentState;
import me.whereareiam.identica.verification.process.VerificationStepCursor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class TotpEnrollmentState implements VerificationEnrollmentState, VerificationStepCursor {
	private @NotNull String stepId;
	private @NotNull String secret;
	private @NotNull String uri;
	@Builder.Default
	private @NotNull List<String> recoveryCodes = new ArrayList<>();
	private boolean verified;

	@Override
	public String credential() {
		return secret;
	}

	@Override
	public String label() {
		return "Authenticator App";
	}
}
