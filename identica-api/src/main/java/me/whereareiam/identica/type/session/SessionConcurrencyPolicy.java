package me.whereareiam.identica.type.session;

/**
 * Policy for handling concurrent sessions for the same account.
 */
@SuppressWarnings("unused")
public enum SessionConcurrencyPolicy {
	/**
	 * Allow multiple sessions for the same account.
	 */
	ALLOW_MULTIPLE,
	/**
	 * Replace the existing session when a new one is opened.
	 */
	REPLACE_EXISTING,
	/**
	 * Reject opening a new session when one already exists.
	 */
	REJECT_NEW;

	public boolean allowsMultiple() {
		return this == ALLOW_MULTIPLE;
	}

	public boolean replacesExisting() {
		return this == REPLACE_EXISTING;
	}

	public boolean rejectsNew() {
		return this == REJECT_NEW;
	}
}
