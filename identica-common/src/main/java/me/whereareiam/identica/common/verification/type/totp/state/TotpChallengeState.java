package me.whereareiam.identica.common.verification.type.totp.state;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import me.whereareiam.identica.model.verification.challenge.VerificationChallengeState;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class TotpChallengeState implements VerificationChallengeState {
	private String stepId;
	private int attempts;
	private boolean recoveryCodeUsed;
}
