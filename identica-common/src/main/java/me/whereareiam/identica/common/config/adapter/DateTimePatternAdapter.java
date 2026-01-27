package me.whereareiam.identica.common.config.adapter;

import me.whereareiam.configura.TypeAdapter;
import me.whereareiam.identica.model.config.DateTimePattern;
import org.jetbrains.annotations.Nullable;

public class DateTimePatternAdapter implements TypeAdapter<DateTimePattern> {
	@Override
	public @Nullable DateTimePattern deserialize(@Nullable String value) {
		if (value == null) return null;

		String pattern = value.trim();
		if (pattern.isEmpty()) return null;

		return new DateTimePattern(pattern);
	}

	@Override
	public @Nullable String serialize(@Nullable DateTimePattern value) {
		if (value == null) return null;
		return value.getPattern();
	}
}
