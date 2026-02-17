package me.whereareiam.identica.type.ratelimit;

/**
 * Connection entry points where a rate limit can be enforced.
 */
public enum RateLimitScope {
	PROCESS,
	RESUME,
	ADVANCE
}
