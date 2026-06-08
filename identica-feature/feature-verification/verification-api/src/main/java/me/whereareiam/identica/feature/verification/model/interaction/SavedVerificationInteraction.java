package me.whereareiam.identica.feature.verification.model.interaction;

import lombok.*;
import me.whereareiam.identica.feature.verification.VerificationInteraction;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Confirmation that a user saved recovery codes or setup material.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class SavedVerificationInteraction implements VerificationInteraction {
	private @NotNull UUID subjectUniqueId;

	@Override
	public @NotNull UUID subjectUniqueId() {
		return subjectUniqueId;
	}
}
