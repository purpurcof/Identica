package me.whereareiam.identica.model.conflict;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Result of conflict resolution.
 */
@Getter
@ToString
@RequiredArgsConstructor
@SuppressWarnings("unused")
public class ConflictResolution {
	private final @NotNull Action action;
	private final @Nullable String overrideValue;
	private final @NotNull OverrideTarget overrideTarget;
	private final @Nullable String message;

	/**
	 * Resolution actions.
	 */
	public enum Action {
		ALLOW,
		PASS,
		DENY,
		KICK_EXISTING,
		KICK_BOTH
	}

	/**
	 * Targets for override values.
	 */
	public enum OverrideTarget {
		INCOMING,
		EXISTING,
		BOTH
	}

	/**
	 * Allow the candidate value as-is.
	 *
	 * @return allow resolution
	 */
	public static ConflictResolution allow() {
		return new ConflictResolution(Action.ALLOW, null, OverrideTarget.INCOMING, null);
	}

	/**
	 * Pass to the next resolver.
	 *
	 * @return pass resolution
	 */
	public static ConflictResolution pass() {
		return new ConflictResolution(Action.PASS, null, OverrideTarget.INCOMING, null);
	}

	/**
	 * Allow the candidate value with an override.
	 *
	 * @param overrideValue replacement value to apply
	 * @return allow resolution with override
	 */
	public static ConflictResolution allowWithOverride(@Nullable String overrideValue) {
		return new ConflictResolution(Action.ALLOW, overrideValue, OverrideTarget.INCOMING, null);
	}

	/**
	 * Allow the candidate value with an override.
	 *
	 * @param overrideValue replacement value to apply
	 * @param target target to apply the override to
	 * @return allow resolution with override
	 */
	public static ConflictResolution allowWithOverride(
			@Nullable String overrideValue,
			@NotNull OverrideTarget target
	) {
		return new ConflictResolution(Action.ALLOW, overrideValue, target, null);
	}

	/**
	 * Deny the operation.
	 *
	 * @param message optional message
	 * @return deny resolution
	 */
	public static ConflictResolution deny(@Nullable String message) {
		return new ConflictResolution(Action.DENY, null, OverrideTarget.INCOMING, message);
	}

	/**
	 * Kick the existing session and allow the joiner.
	 *
	 * @param overrideValue optional override value
	 * @return kick-existing resolution
	 */
	public static ConflictResolution kickExisting(@Nullable String overrideValue) {
		return new ConflictResolution(Action.KICK_EXISTING, overrideValue, OverrideTarget.INCOMING, null);
	}

	/**
	 * Kick both sessions and deny the joiner.
	 *
	 * @param message optional message
	 * @return kick-both resolution
	 */
	public static ConflictResolution kickBoth(@Nullable String message) {
		return new ConflictResolution(Action.KICK_BOTH, null, OverrideTarget.INCOMING, message);
	}
}
