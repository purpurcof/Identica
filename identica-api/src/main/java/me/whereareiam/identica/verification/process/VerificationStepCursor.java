package me.whereareiam.identica.verification.process;

import org.jetbrains.annotations.NotNull;

/**
 * Mutable cursor contract for process states that persist the current step id.
 *
 * <p>States can implement this interface to opt into the default reusable
 * verification process orchestrator.</p>
 */
public interface VerificationStepCursor {
	/**
	 * Returns the current persisted step id.
	 *
	 * @return current step id
	 */
	@NotNull String getStepId();

	/**
	 * Updates the current persisted step id.
	 *
	 * @param stepId next step id
	 */
	void setStepId(@NotNull String stepId);
}
