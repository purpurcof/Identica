package me.whereareiam.identica.model.config.type;

import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Wrapper for a date-time formatter pattern stored in configuration.
 *
 * <p>Example:
 * <pre>{@code
 * DateTimePattern pattern = new DateTimePattern("dd.MM.yyyy");
 * DateTimeFormatter formatter = pattern.formatter(DateTimeFormatter.ISO_LOCAL_DATE);
 * }</pre>
 */
@ToString
@EqualsAndHashCode
@RequiredArgsConstructor
public final class DateTimePattern {
	private final @NotNull String pattern;
	private transient volatile DateTimeFormatter formatter;

	/**
	 * Returns the raw formatter pattern from configuration.
	 *
	 * @return the configured pattern
	 */
	public @NotNull String getPattern() {
		return pattern;
	}

	/**
	 * Builds or returns a cached formatter for this pattern.
	 *
	 * @param fallback formatter used when the pattern is blank
	 * @return formatter using the system default zone
	 */
	public @NotNull DateTimeFormatter formatter(@NotNull DateTimeFormatter fallback) {
		if (pattern.isBlank()) return fallback.withZone(ZoneId.systemDefault());

		DateTimeFormatter cached = formatter;
		if (cached != null) return cached;

		DateTimeFormatter created = DateTimeFormatter.ofPattern(pattern).withZone(ZoneId.systemDefault());
		formatter = created;
		return created;
	}
}
