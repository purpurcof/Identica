package me.whereareiam.identica.common.config.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;
import me.whereareiam.identica.model.config.type.DateTimePattern;

import java.io.IOException;

public final class DateTimePatternSerializer extends StdScalarSerializer<DateTimePattern> {
	public DateTimePatternSerializer() {
		super(DateTimePattern.class);
	}

	@Override
	public void serialize(DateTimePattern value, JsonGenerator gen, SerializerProvider provider) throws IOException {
		if (value == null) {
			gen.writeNull();
			return;
		}

		gen.writeString(value.getPattern());
	}
}
