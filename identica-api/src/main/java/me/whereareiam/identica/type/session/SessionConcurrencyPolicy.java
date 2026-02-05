package me.whereareiam.identica.type.session;

/**
 * Policy for handling concurrent sessions for the same account.
 */
public enum SessionConcurrencyPolicy {
	/**
	 * Allow multiple sessions for the same account.
	 */
	ALLOW_MULTI,
	/**
	 * Kick the existing session when a new one is opened.
	 */
	KICK_EXISTING,
	/**
	 * Deny opening a new session when one already exists.
	 */
	DENY_NEW
}
