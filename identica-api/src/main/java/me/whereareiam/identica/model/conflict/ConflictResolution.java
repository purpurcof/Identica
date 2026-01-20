package me.whereareiam.identica.model.conflict;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Result of conflict resolution returned by providers.
 */
@Getter
@AllArgsConstructor
public class ConflictResolution {
	private final ConflictAction action;
	private final String originalUsername;
	private final String effectiveUsername;
	private final String denyMessage;

	public enum ConflictAction {
		ALLOW,
		ALLOW_MODIFIED,
		DENY
	}

	public static ConflictResolution allow() {
		return new ConflictResolution(ConflictAction.ALLOW, null, null, null);
	}

	public static ConflictResolution allowModified(String original, String effective) {
		return new ConflictResolution(ConflictAction.ALLOW_MODIFIED, original, effective, null);
	}

	public static ConflictResolution deny(String message) {
		return new ConflictResolution(ConflictAction.DENY, null, null, message);
	}
}
