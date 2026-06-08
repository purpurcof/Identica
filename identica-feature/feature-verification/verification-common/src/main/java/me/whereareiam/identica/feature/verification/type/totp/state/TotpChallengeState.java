package me.whereareiam.identica.feature.verification.type.totp.state;

import lombok.*;
import me.whereareiam.identica.feature.verification.state.VerificationChallengeState;

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
