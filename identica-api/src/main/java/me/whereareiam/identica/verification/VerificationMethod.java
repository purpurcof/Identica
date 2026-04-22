package me.whereareiam.identica.verification;

import me.whereareiam.identica.model.verification.VerificationMethodDescriptor;
import org.jetbrains.annotations.NotNull;

/**
 * Public contract for a verification method.
 *
 * <p>Methods provide their own enrollment and challenge processes. Identica
 * core decides when a method is allowed for a provider and persists the
 * resulting enrollment, selection, and challenge state.</p>
 */
public interface VerificationMethod {
	/**
	 * Returns stable method metadata used for configuration, commands, and UI.
	 *
	 * @return method descriptor
	 */
	@NotNull VerificationMethodDescriptor descriptor();

	/**
	 * Returns the enrollment process for this method.
	 *
	 * @return enrollment process
	 */
	@NotNull VerificationEnrollmentProcess<?> enrollment();

	/**
	 * Returns the challenge process for this method.
	 *
	 * @return challenge process
	 */
	@NotNull VerificationChallengeProcess<?> challenge();

}
