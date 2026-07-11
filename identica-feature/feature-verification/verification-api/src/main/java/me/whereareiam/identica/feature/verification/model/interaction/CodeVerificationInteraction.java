package me.whereareiam.identica.feature.verification.model.interaction;

import lombok.*;
import me.whereareiam.identica.feature.verification.VerificationInteraction;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Command-entered verification code interaction.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class CodeVerificationInteraction implements VerificationInteraction {
	private @NotNull UUID subjectUniqueId;
	private @NotNull String code;

	@Override
	public @NotNull UUID subjectUniqueId() {
		return subjectUniqueId;
	}
}
