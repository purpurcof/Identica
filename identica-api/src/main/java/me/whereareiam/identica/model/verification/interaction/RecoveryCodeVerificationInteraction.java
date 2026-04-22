package me.whereareiam.identica.model.verification.interaction;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import me.whereareiam.identica.verification.VerificationInteraction;
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
