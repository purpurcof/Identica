package me.whereareiam.identica.model.ratelimit;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.util.Locale;

@Getter
@ToString
@RequiredArgsConstructor
public class RateLimitKey {
	private final String key;

	public boolean isBlank() {
		return key == null || key.isBlank();
	}

	public static RateLimitKey ipAndIdentity(RateLimitContext ctx) {
		if (ctx == null) return new RateLimitKey(null);

		String ip = normalize(ctx.getIp());
		String identity;
		if (ctx.getUniqueId() != null) {
			identity = ctx.getUniqueId().toString();
		} else {
			identity = normalize(ctx.getUsername());
		}

		if (identity == null && ctx.getConnectionUniqueId() != null)
			identity = ctx.getConnectionUniqueId().toString();

		if (ip == null && identity == null) return new RateLimitKey(null);
		if (ip == null) return new RateLimitKey(identity);
		if (identity == null) return new RateLimitKey(ip);

		return new RateLimitKey(ip + "|" + identity);
	}

	private static String normalize(String value) {
		if (value == null || value.isBlank()) return null;
		return value.trim().toLowerCase(Locale.ROOT);
	}
}
