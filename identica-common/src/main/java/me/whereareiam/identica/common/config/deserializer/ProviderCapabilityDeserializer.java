package me.whereareiam.identica.common.config.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import me.whereareiam.identica.type.provider.ProviderCapability;

import java.io.IOException;

public final class ProviderCapabilityDeserializer extends StdScalarDeserializer<ProviderCapability> {
	public ProviderCapabilityDeserializer() {
		super(ProviderCapability.class);
	}

	@Override
	public ProviderCapability deserialize(JsonParser parser, DeserializationContext context) throws IOException {
		String value = parser.getValueAsString();
		ProviderCapability capability = ProviderCapability.fromId(value);

		if (capability == null) throw new IllegalArgumentException("Unknown provider capability: " + value);
		return capability;
	}
}
