package me.whereareiam.identica.common.config;

import com.fasterxml.jackson.databind.module.SimpleModule;
import me.whereareiam.identica.common.config.deserializer.DateTimePatternDeserializer;
import me.whereareiam.identica.common.config.serializer.DateTimePatternSerializer;
import me.whereareiam.identica.model.config.type.DateTimePattern;

public class IdenticaModule extends SimpleModule {
	public IdenticaModule() {
		addSerializer(DateTimePattern.class, new DateTimePatternSerializer());
		addDeserializer(DateTimePattern.class, new DateTimePatternDeserializer());
	}
}
