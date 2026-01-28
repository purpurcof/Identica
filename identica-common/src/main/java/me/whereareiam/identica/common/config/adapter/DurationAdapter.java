package me.whereareiam.identica.common.config.adapter;

import me.whereareiam.configura.TypeAdapter;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DurationAdapter implements TypeAdapter<Duration> {
	private static final Pattern TOKEN = Pattern.compile("(\\d+)(ms|d|h|m|s)");

	@Override
	public @Nullable Duration deserialize(@Nullable String value) {
		if (value == null) return null;
		String raw = value.trim();
		if (raw.isEmpty()) return null;

		String normalized = raw.toLowerCase(Locale.ROOT).replace(" ", "");
		if (isDigits(normalized)) {
			return Duration.ofMinutes(Long.parseLong(normalized));
		}

		Duration parsed = parseTokens(normalized);
		if (parsed != null) return parsed;

		try {
			return Duration.parse(normalized.toUpperCase(Locale.ROOT));
		} catch (DateTimeParseException e) {
			throw new IllegalArgumentException("Invalid duration value: " + value, e);
		}
	}

	@Override
	public @Nullable String serialize(@Nullable Duration value) {
		if (value == null) return null;
		if (value.isZero()) return "0m";
		if (value.isNegative()) return value.toString();

		long days = value.toDaysPart();
		long hours = value.toHoursPart();
		long minutes = value.toMinutesPart();
		long seconds = value.toSecondsPart();
		long millis = value.toMillisPart();

		StringBuilder builder = new StringBuilder();
		append(builder, days, "d");
		append(builder, hours, "h");
		append(builder, minutes, "m");
		append(builder, seconds, "s");
		append(builder, millis, "ms");

		return !builder.isEmpty() ? builder.toString() : "0m";
	}

	private Duration parseTokens(String normalized) {
		Matcher matcher = TOKEN.matcher(normalized);
		int index = 0;
		Duration total = Duration.ZERO;
		boolean matched = false;

		while (matcher.find()) {
			if (matcher.start() != index)
				return null;

			matched = true;
			long amount = Long.parseLong(matcher.group(1));
			String unit = matcher.group(2);
			total = total.plus(toDuration(amount, unit));
			index = matcher.end();
		}

		if (!matched || index != normalized.length()) {
			return null;
		}
		return total;
	}

	private boolean isDigits(String value) {
		if (value.isEmpty()) return false;
		for (int i = 0; i < value.length(); i++)
			if (!Character.isDigit(value.charAt(i))) return false;

		return true;
	}

	private Duration toDuration(long amount, String unit) {
		return switch (unit) {
			case "d" -> Duration.ofDays(amount);
			case "h" -> Duration.ofHours(amount);
			case "m" -> Duration.ofMinutes(amount);
			case "s" -> Duration.ofSeconds(amount);
			case "ms" -> Duration.ofMillis(amount);
			default -> Duration.ZERO;
		};
	}

	private void append(StringBuilder builder, long value, String unit) {
		if (value <= 0) return;
		builder.append(value).append(unit);
	}
}
