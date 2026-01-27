package me.whereareiam.identica.util;

import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.SplittableRandom;
import java.util.UUID;

/**
 * Utility for generating Identica and offline-mode UUIDs.
 */
public final class UniqueIdGenerator {
	private static final SplittableRandom RNG = new SplittableRandom();

	/**
	 * Generates a new Identica unique id.
	 *
	 * @return generated unique id
	 */
	public static UUID newIdenticaUniqueId() {
		long msb = RNG.nextLong();
		long lsb = RNG.nextLong();

		lsb = (lsb & 0x3fffffffffffffffL) | 0x8000000000000000L;
		msb = (msb & 0xffffffffffff0fffL) | 0x0000000000008000L;

		return new UUID(msb, lsb);
	}

	/**
	 * Generates an offline-mode UUID using the standard OfflinePlayer namespace.
	 *
	 * @param username player username
	 * @return derived UUID or {@code null} when the username is blank
	 */
	public static @Nullable UUID offlinePlayerUniqueId(@Nullable String username) {
		if (username == null || username.isBlank()) return null;
		return UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes(StandardCharsets.UTF_8));
	}
}
