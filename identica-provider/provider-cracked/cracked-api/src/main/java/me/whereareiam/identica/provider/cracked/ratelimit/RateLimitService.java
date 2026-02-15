package me.whereareiam.identica.provider.cracked.ratelimit;

import me.whereareiam.identica.provider.cracked.model.RateLimitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Service for tracking authentication rate limits for the cracked provider.
 */
public interface RateLimitService {
	/**
	 * Return the remaining rate limit time for the supplied IP address.
	 *
	 * @param ip remote IP address
	 * @return remaining rate limit seconds, or {@code 0} when not limited
	 */
	long remainingLimitSeconds(@Nullable String ip);

	/**
	 * Record a failed authentication attempt and return the new rate limit state.
	 *
	 * @param ip remote IP address
	 * @param maxAttempts maximum attempts before limiting
	 * @param lockSeconds limit duration in seconds
	 * @return rate limit result after recording the failure
	 */
	@NotNull RateLimitResult recordFailure(
			@Nullable String ip,
			int maxAttempts,
			int lockSeconds
	);

	/**
	 * Clear the rate limit state for the supplied IP address.
	 *
	 * @param ip remote IP address
	 */
	void clear(@Nullable String ip);

}
