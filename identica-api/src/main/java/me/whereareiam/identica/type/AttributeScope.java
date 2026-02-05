package me.whereareiam.identica.type;

import me.whereareiam.identica.attributes.ScopedAttributes;

/**
 * Scope identifiers for {@link ScopedAttributes} storage.
 */
public enum AttributeScope {
	/**
	 * Short-lived profile rewrite hints keyed by username.
	 */
	PROFILE_HINT,
	/**
	 * Attempt-scoped data keyed by attempt id.
	 */
	ATTEMPT,
	/**
	 * Identity-scoped data keyed by unique id.
	 */
	IDENTITY,
	/**
	 * Session-scoped data keyed by session id.
	 */
	SESSION
}
