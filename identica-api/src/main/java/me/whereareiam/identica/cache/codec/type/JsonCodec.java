package me.whereareiam.identica.cache.codec.type;

import me.whereareiam.configura.Config;
import me.whereareiam.configura.reader.ConfigReader;
import me.whereareiam.configura.type.Format;
import me.whereareiam.configura.writer.ConfigWriter;
import me.whereareiam.identica.cache.codec.CacheCodec;

public final class JsonCodec<T> implements CacheCodec<T> {
	private static final ConfigReader JSON_READER = Config.getDefaultReader().withFormat(Format.JSON);
	private static final ConfigWriter JSON_WRITER = Config.getDefaultWriter().withFormat(Format.JSON);

	private final Class<T> type;

	public JsonCodec(Class<T> type) {
		this.type = type;
	}

	public static <T> JsonCodec<T> of(Class<T> type) {
		return new JsonCodec<>(type);
	}

	@Override
	public byte[] encode(T value) {
		return JSON_WRITER.encode(value);
	}

	@Override
	public T decode(byte[] data) {
		return JSON_READER.decode(data, type);
	}
}
