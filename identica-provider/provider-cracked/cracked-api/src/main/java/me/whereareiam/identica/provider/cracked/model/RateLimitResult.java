package me.whereareiam.identica.provider.cracked.model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

/**
 * Result of an authentication rate limit attempt.
 */
@Getter
@ToString
@EqualsAndHashCode
@AllArgsConstructor
public class RateLimitResult {
	private final boolean limited;
	private final long remainingSeconds;
	private final int attempts;

	public static @NotNull RateLimitResult none() {
		return new RateLimitResult(false, 0, 0);
	}

	public static @NotNull RateLimitResult attempts(int attempts) {
		return new RateLimitResult(false, 0, attempts);
	}

	public static @NotNull RateLimitResult limited(long remainingSeconds) {
		return new RateLimitResult(true, remainingSeconds, 0);
	}

	public boolean limited() {
		return limited;
	}

	public long remainingSeconds() {
		return remainingSeconds;
	}

	public int attempts() {
		return attempts;
	}
}
