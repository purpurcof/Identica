package me.whereareiam.identica.feature.sentinel.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import me.whereareiam.identica.feature.sentinel.model.SentinelContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * Normalized storage key used for grouping sentinel state.
 */
@Getter
@ToString
@RequiredArgsConstructor
public class SentinelKey {
	private final @Nullable String key;

	/**
	 * Returns whether the key is absent or blank.
	 *
	 * @return {@code true} when the key cannot be used for storage
	 */
	public boolean isBlank() {
		return key == null || key.isBlank();
	}

	/**
	 * Builds a key from IP and identity information.
	 *
	 * @param ctx current sentinel context
	 * @return combined storage key
	 */
	public static @NotNull SentinelKey ipAndIdentity(@Nullable SentinelContext ctx) {
		if (ctx == null) return new SentinelKey(null);

		String ip = normalize(ctx.getIp());
		String identity;
		if (ctx.getUniqueId() != null) {
			identity = ctx.getUniqueId().toString();
		} else {
			identity = normalize(ctx.getUsername());
		}

		if (identity == null && ctx.getConnectionUniqueId() != null)
			identity = ctx.getConnectionUniqueId().toString();

		if (ip == null && identity == null) return new SentinelKey(null);
		if (ip == null) return new SentinelKey(identity);
		if (identity == null) return new SentinelKey(ip);

		return new SentinelKey(ip + "|" + identity);
	}

	private static @Nullable String normalize(@Nullable String value) {
		if (value == null || value.isBlank()) return null;
		return value.trim().toLowerCase(Locale.ROOT);
	}
}
