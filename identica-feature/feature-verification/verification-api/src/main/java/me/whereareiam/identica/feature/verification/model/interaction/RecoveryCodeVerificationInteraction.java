package me.whereareiam.identica.feature.verification.model.interaction;

import lombok.*;
import me.whereareiam.identica.feature.verification.VerificationInteraction;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Explicit recovery-code verification interaction.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class RecoveryCodeVerificationInteraction implements VerificationInteraction {
	private @NotNull UUID subjectUniqueId;
	private @NotNull String code;

	@Override
	public @NotNull UUID subjectUniqueId() {
		return subjectUniqueId;
	}
}
