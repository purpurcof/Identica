package me.whereareiam.identica.common.config.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import me.whereareiam.identica.model.config.DateTimePattern;

import java.io.IOException;

public final class DateTimePatternDeserializer extends StdScalarDeserializer<DateTimePattern> {
	public DateTimePatternDeserializer() {
		super(DateTimePattern.class);
	}

	@Override
	public DateTimePattern deserialize(JsonParser parser, DeserializationContext context) throws IOException {
		String value = parser.getValueAsString();
		if (value == null) return null;

		String pattern = value.trim();
		return pattern.isEmpty()
				? null
				: new DateTimePattern(pattern);
	}
}
