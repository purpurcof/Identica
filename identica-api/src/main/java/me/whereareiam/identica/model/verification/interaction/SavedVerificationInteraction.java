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
