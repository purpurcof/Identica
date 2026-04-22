package me.whereareiam.identica.verification;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Marker for typed verification interactions.
 *
 * <p>Interactions represent any user or external action that can advance a
 * verification process, such as a command-entered code, a saved confirmation,
 * or a third-party plugin callback.</p>
 */
public interface VerificationInteraction {
	/**
	 * Returns the Identica subject that owns the interaction.
	 *
	 * @return subject unique id
	 */
	@NotNull UUID subjectUniqueId();
}
