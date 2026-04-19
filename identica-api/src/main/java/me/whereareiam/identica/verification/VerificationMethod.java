package me.whereareiam.identica.verification;

import me.whereareiam.identica.model.config.Verification;
import me.whereareiam.identica.model.verification.enrollment.PendingVerificationEnrollment;
import me.whereareiam.identica.model.verification.VerificationActionResult;
import me.whereareiam.identica.type.verification.VerificationActionStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * Handler for a concrete verification method implementation.
 *
 * <p>Method handlers own the method-specific logic for enrollment bootstrap,
 * code validation, challenge verification, and recovery-code generation.</p>
 */
public interface VerificationMethod {
	/**
	 * Returns the stable method id.
	 *
	 * @return method id
	 */
	@NotNull String id();

	/**
	 * Returns the user-facing display name for this method.
	 *
	 * @param config shared verification configuration
	 * @return display name
	 */
	default @NotNull String displayName(@NotNull Verification config) {
		return id();
	}

	/**
	 * Starts a new enrollment for the method.
	 *
	 * @param uniqueId Identica identity id
	 * @param username current username used for display labels
	 * @param providerId optional provider context for the enrollment
	 * @param config shared verification configuration
	 * @return pending enrollment state
	 */
	@NotNull PendingVerificationEnrollment beginEnrollment(
			@NotNull UUID uniqueId,
			@NotNull String username,
			@Nullable String providerId,
			@NotNull Verification config
	);

	/**
	 * Verifies a method-specific enrollment confirmation input.
	 *
	 * @param pending pending enrollment state
	 * @param input user-provided input
	 * @param config shared verification configuration
	 * @return {@code true} when the enrollment confirmation is valid
	 */
	boolean verifyEnrollment(
			@NotNull PendingVerificationEnrollment pending,
			@NotNull String input,
			@NotNull Verification config
	);

	/**
	 * Verifies a login-time challenge input.
	 *
	 * @param payload persisted method payload
	 * @param input user-provided challenge input
	 * @param config shared verification configuration
	 * @return {@code true} when the challenge input is valid
	 */
	boolean verifyChallenge(
			@NotNull String payload,
			@NotNull String input,
			@NotNull Verification config
	);

	/**
	 * Generates recovery codes for a newly completed enrollment.
	 *
	 * @param config shared verification configuration
	 * @return generated recovery codes
	 */
	@NotNull List<String> generateRecoveryCodes(@NotNull Verification config);

	/**
	 * Creates a standard unavailable-method action result.
	 *
	 * @param providerId optional provider context
	 * @return unavailable-method action result
	 */
	default @NotNull VerificationActionResult unavailableResult(@Nullable String providerId) {
		return VerificationActionResult.builder()
				.status(VerificationActionStatus.METHOD_UNAVAILABLE)
				.providerId(providerId)
				.methodId(id())
				.build();
	}
}
