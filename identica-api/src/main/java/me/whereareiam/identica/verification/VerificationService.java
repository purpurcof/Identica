package me.whereareiam.identica.verification;

import me.whereareiam.identica.model.verification.enrollment.VerificationEnrollmentSession;
import me.whereareiam.identica.model.verification.VerificationAttemptResult;
import me.whereareiam.identica.model.verification.VerificationDisableResult;
import me.whereareiam.identica.model.verification.VerificationResetResult;
import me.whereareiam.identica.model.verification.selection.VerificationSelectionResult;
import me.whereareiam.identica.model.verification.enrollment.VerificationEnrollment;
import me.whereareiam.identica.model.verification.enrollment.VerificationEnrollmentResult;
import me.whereareiam.identica.model.verification.selection.VerificationSelection;
import me.whereareiam.identica.model.verification.VerificationTarget;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for verification enrollment, selection, and authentication challenges.
 *
 * <p>This API owns the user-facing verification lifecycle: enrolling methods,
 * confirming pending enrollment steps, selecting methods per provider, resetting
 * state, and validating login-time verification input.</p>
 */
public interface VerificationService {
	/**
	 * Starts enrollment for a verification method.
	 *
	 * @param uniqueId Identica identity id
	 * @param username current player username used for labels or prompts
	 * @param providerId optional provider context for the enrollment
	 * @param methodId method id to enroll
	 * @return enrollment result describing whether the workflow started or why it could not
	 */
	@NotNull VerificationEnrollmentResult beginEnrollment(
			@NotNull UUID uniqueId,
			@NotNull String username,
			@Nullable String providerId,
			@NotNull String methodId
	);

	/**
	 * Confirms a pending enrollment step.
	 *
	 * <p>The input may be a method-specific verification code or a special
	 * confirmation value such as {@code saved}, depending on the current pending
	 * enrollment stage.</p>
	 *
	 * @param uniqueId Identica identity id
	 * @param value confirmation input
	 * @return enrollment result describing the next enrollment state
	 */
	@NotNull VerificationEnrollmentResult confirmEnrollment(
			@NotNull UUID uniqueId,
			@NotNull String value
	);

	/**
	 * Cancels any pending enrollment for the identity.
	 *
	 * @param uniqueId Identica identity id
	 * @return {@code true} when a pending enrollment was cancelled
	 */
	boolean cancelPendingEnrollment(@NotNull UUID uniqueId);

	/**
	 * Finds the pending enrollment for the identity.
	 *
	 * @param uniqueId Identica identity id
	 * @return pending enrollment, or empty when none exists
	 */
	@NotNull Optional<VerificationEnrollmentSession> findPendingEnrollment(@NotNull UUID uniqueId);

	/**
	 * Returns all enrolled verification methods for the identity.
	 *
	 * @param uniqueId Identica identity id
	 * @return enrolled methods
	 */
	@NotNull List<VerificationEnrollment> findEnrollments(@NotNull UUID uniqueId);

	/**
	 * Returns all provider-specific method selections for the identity.
	 *
	 * @param uniqueId Identica identity id
	 * @return selected methods keyed by provider
	 */
	@NotNull List<VerificationSelection> findSelections(@NotNull UUID uniqueId);

	/**
	 * Selects the active verification method for a provider.
	 *
	 * @param uniqueId Identica identity id
	 * @param providerId provider id to bind the method to
	 * @param methodId method id to select
	 * @return selection result describing the update outcome
	 */
	@NotNull VerificationSelectionResult selectMethod(
			@NotNull UUID uniqueId,
			@NotNull String providerId,
			@NotNull String methodId
	);

	/**
	 * Disables an enrolled verification method for the identity.
	 *
	 * @param uniqueId Identica identity id
	 * @param methodId method id to disable
	 * @return disable result describing whether the method was removed
	 */
	@NotNull VerificationDisableResult disableMethod(
			@NotNull UUID uniqueId,
			@NotNull String methodId
	);

	/**
	 * Resets verification state for the identity.
	 *
	 * <p>When {@code providerId} is supplied, only provider-specific selections are
	 * reset. When it is {@code null}, all verification state for the identity is
	 * cleared.</p>
	 *
	 * @param uniqueId Identica identity id
	 * @param providerId optional provider id to scope the reset
	 * @return reset result describing the completed reset scope
	 */
	@NotNull VerificationResetResult reset(
			@NotNull UUID uniqueId,
			@Nullable String providerId
	);

	/**
	 * Resolves the verification state for a target without applying user input.
	 *
	 * @param target verification target
	 * @return resolution result
	 */
	@NotNull VerificationAttemptResult resolve(@NotNull VerificationTarget target);

	/**
	 * Verifies user input against a target.
	 *
	 * @param target verification target
	 * @param input optional challenge input
	 * @return verification result
	 */
	@NotNull VerificationAttemptResult verify(
			@NotNull VerificationTarget target,
			@Nullable String input
	);

	default @NotNull VerificationAttemptResult attempt(
			@NotNull UUID uniqueId,
			@NotNull String providerId,
			@Nullable String input
	) {
		return verify(VerificationTarget.providerSelection(uniqueId, providerId, "provider-selection"), input);
	}

	default @NotNull VerificationAttemptResult verifyMethod(
			@NotNull UUID uniqueId,
			@NotNull String methodId,
			@Nullable String input
	) {
		return verify(VerificationTarget.methodEnrollment(uniqueId, methodId, null, "method-enrollment"), input);
	}
}
