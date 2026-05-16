package me.whereareiam.identica.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

/**
 * Utility helpers for safely parsing UUID-based unique ids.
 */
public final class UniqueIdUtil {
	/**
	 * Parses a UUID value after trimming surrounding whitespace.
	 *
	 * @param value raw value to parse
	 * @return parsed UUID or {@code null} when the value is blank or invalid
	 */
	public static @Nullable UUID parseUniqueId(@Nullable String value) {
		if (value == null || value.isBlank()) return null;

		try {
			return UUID.fromString(value.trim());
		} catch (IllegalArgumentException ignored) {
			return null;
		}
	}

	/**
	 * Parses a UUID value after trimming surrounding whitespace.
	 *
	 * @param value raw value to parse
	 * @return parsed UUID wrapped in an {@link Optional}, or an empty optional when the value is blank or invalid
	 */
	public static @NotNull Optional<UUID> parseOptionalUniqueId(@Nullable String value) {
		return Optional.ofNullable(parseUniqueId(value));
	}
}
