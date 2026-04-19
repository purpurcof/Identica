package me.whereareiam.identica.verification;

import me.whereareiam.identica.model.verification.PendingVerificationEnrollment;
import me.whereareiam.identica.model.verification.VerificationActionResult;
import me.whereareiam.identica.model.verification.VerificationChallengeResult;
import me.whereareiam.identica.model.verification.VerificationEnrollment;
import me.whereareiam.identica.model.verification.VerificationSelection;
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
	 * @return enrollment start result
	 */
	@NotNull VerificationActionResult beginEnrollment(
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
	 * @return confirmation result
	 */
	@NotNull VerificationActionResult confirmEnrollment(
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
	@NotNull Optional<PendingVerificationEnrollment> findPendingEnrollment(@NotNull UUID uniqueId);

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
	 * @return selection result
	 */
	@NotNull VerificationActionResult selectMethod(
			@NotNull UUID uniqueId,
			@NotNull String providerId,
			@NotNull String methodId
	);

	/**
	 * Disables an enrolled verification method for the identity.
	 *
	 * @param uniqueId Identica identity id
	 * @param methodId method id to disable
	 * @return disable result
	 */
	@NotNull VerificationActionResult disableMethod(
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
	 * @return reset result
	 */
	@NotNull VerificationActionResult reset(
			@NotNull UUID uniqueId,
			@Nullable String providerId
	);

	/**
	 * Evaluates a login-time verification challenge.
	 *
	 * @param uniqueId Identica identity id
	 * @param providerId provider id currently authenticating the user
	 * @param input optional challenge input
	 * @return challenge evaluation result
	 */
	@NotNull VerificationChallengeResult challenge(
			@NotNull UUID uniqueId,
			@NotNull String providerId,
			@Nullable String input
	);
}
