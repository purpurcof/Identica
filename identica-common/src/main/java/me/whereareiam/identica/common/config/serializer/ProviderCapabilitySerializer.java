package me.whereareiam.identica.common.config.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;
import me.whereareiam.identica.type.provider.ProviderCapability;

import java.io.IOException;

public final class ProviderCapabilitySerializer extends StdScalarSerializer<ProviderCapability> {
	public ProviderCapabilitySerializer() {
		super(ProviderCapability.class);
	}

	@Override
	public void serialize(ProviderCapability value, JsonGenerator gen, SerializerProvider provider) throws IOException {
		if (value == null) {
			gen.writeNull();
			return;
		}

		gen.writeString(value.getId());
	}
}
