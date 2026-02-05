package me.whereareiam.identica.auth.attempt;

import me.whereareiam.identica.model.auth.request.ResumeRequest;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;

/**
 * Store for pending authentication attempts.
 */
@SuppressWarnings("unused")
public interface AuthAttemptStore {
	/**
	 * Stores a pending attempt with a time-to-live.
	 *
	 * @param attempt attempt snapshot
	 * @param ttlMs time-to-live in milliseconds
	 */
	void store(@NotNull AuthAttempt attempt, long ttlMs);

	/**
	 * Finds a pending attempt by attempt id.
	 *
	 * @param attemptId attempt unique id
	 * @return optional attempt
	 */
	@NotNull Optional<AuthAttempt> find(@NotNull UUID attemptId);

	/**
	 * Consumes a pending attempt matching the resume request.
	 *
	 * @param request resume request
	 * @return optional attempt
	 */
	@NotNull Optional<AuthAttempt> consume(@NotNull ResumeRequest request);

	/**
	 * Returns whether a pending attempt exists for the resume request.
	 *
	 * @param request resume request
	 * @return {@code true} when a pending attempt exists
	 */
	boolean hasPending(@NotNull ResumeRequest request);

	/**
	 * Clears a pending attempt by attempt id.
	 *
	 * @param attemptId attempt unique id
	 */
	void clear(@NotNull UUID attemptId);
}
