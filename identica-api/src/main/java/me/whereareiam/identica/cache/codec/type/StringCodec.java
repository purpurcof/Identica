package me.whereareiam.identica.cache.codec.type;

import me.whereareiam.identica.cache.codec.CacheCodec;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;

/**
 * UTF-8 string codec for cache values.
 */
public final class StringCodec implements CacheCodec<String> {
	/**
	 * Encodes the provided string into UTF-8 bytes.
	 *
	 * @param value string value to encode
	 * @return encoded bytes (empty when input is null)
	 */
	@Override
	public byte @NotNull [] encode(@Nullable String value) {
		if (value == null) return new byte[0];
		return value.getBytes(StandardCharsets.UTF_8);
	}

	/**
	 * Decodes UTF-8 bytes into a string.
	 *
	 * @param data UTF-8 bytes
	 * @return decoded string (empty when input is null or empty)
	 */
	@Override
	public @NotNull String decode(byte @Nullable [] data) {
		if (data == null || data.length == 0) return "";
		return new String(data, StandardCharsets.UTF_8);
	}
}
