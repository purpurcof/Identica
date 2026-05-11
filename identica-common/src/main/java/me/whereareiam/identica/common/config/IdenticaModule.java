package me.whereareiam.identica.common.config;

import com.fasterxml.jackson.databind.module.SimpleModule;
import me.whereareiam.identica.common.config.deserializer.DateTimePatternDeserializer;
import me.whereareiam.identica.common.config.deserializer.ProviderCapabilityDeserializer;
import me.whereareiam.identica.model.config.DateTimePattern;
import me.whereareiam.identica.common.config.serializer.DateTimePatternSerializer;
import me.whereareiam.identica.common.config.serializer.ProviderCapabilitySerializer;
import me.whereareiam.identica.type.provider.ProviderCapability;

public class IdenticaModule extends SimpleModule {
	public IdenticaModule() {
		addSerializer(DateTimePattern.class, new DateTimePatternSerializer());
		addDeserializer(DateTimePattern.class, new DateTimePatternDeserializer());
		addSerializer(ProviderCapability.class, new ProviderCapabilitySerializer());
		addDeserializer(ProviderCapability.class, new ProviderCapabilityDeserializer());
	}
}
