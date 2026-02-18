package me.whereareiam.identica.model.sentinel;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.util.Locale;

@Getter
@ToString
@RequiredArgsConstructor
public class SentinelKey {
	private final String key;

	public boolean isBlank() {
		return key == null || key.isBlank();
	}

	public static SentinelKey ipAndIdentity(SentinelContext ctx) {
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

	private static String normalize(String value) {
		if (value == null || value.isBlank()) return null;
		return value.trim().toLowerCase(Locale.ROOT);
	}
}
