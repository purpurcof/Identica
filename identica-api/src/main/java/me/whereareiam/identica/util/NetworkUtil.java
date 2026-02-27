package me.whereareiam.identica.util;

import com.google.common.net.HostAndPort;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.IDN;
import java.util.Locale;

/**
 * Networking-related helpers for host and entrypoint parsing.
 */
public final class NetworkUtil {
	/**
	 * Parses a host or host:port entrypoint string.
	 *
	 * @param raw raw entrypoint string
	 * @return parsed host/port or {@code null} when invalid
	 */
	public static @Nullable ParsedHost parse(@Nullable String raw) {
		if (raw == null) return null;
		String trimmed = raw.trim();
		if (trimmed.isEmpty()) return null;

		HostAndPort parsed;
		try {
			parsed = HostAndPort.fromString(trimmed);
		} catch (IllegalArgumentException ignored) {
			return null;
		}

		String normalizedHost = normalizeHost(parsed.getHost());
		if (normalizedHost == null) return null;

		Integer port = parsed.hasPort() ? parsed.getPort() : null;
		return new ParsedHost(normalizedHost, port);
	}

	/**
	 * Normalizes a host name by applying IDN conversion and lowercasing.
	 *
	 * @param host input host name
	 * @return normalized host or {@code null} when invalid
	 */
	public static @Nullable String normalizeHost(@Nullable String host) {
		if (host == null) return null;
		String trimmed = host.trim();
		if (trimmed.isEmpty()) return null;

		String normalized = trimmed;
		if (!isIpv6Literal(trimmed)) {
			try {
				normalized = IDN.toASCII(trimmed);
			} catch (IllegalArgumentException ignored) {
				return null;
			}
		}

		return normalized.toLowerCase(Locale.ROOT);
	}

	private static boolean isIpv6Literal(@NotNull String host) {
		return host.indexOf(':') >= 0;
	}

	/**
	 * Parsed host entrypoint value.
	 *
	 * @param host normalized host
	 * @param port port when specified, otherwise {@code null}
	 */
	public record ParsedHost(@NotNull String host, @Nullable Integer port) {
	}
}
