package me.whereareiam.identica.util;

import java.util.SplittableRandom;
import java.util.UUID;

public final class UniqueIdGenerator {
	private static final SplittableRandom RNG = new SplittableRandom();

	public static UUID newIdenticaUniqueId() {
		long msb = RNG.nextLong();
		long lsb = RNG.nextLong();

		lsb = (lsb & 0x3fffffffffffffffL) | 0x8000000000000000L;
		msb = (msb & 0xffffffffffff0fffL) | 0x0000000000008000L;

		return new UUID(msb, lsb);
	}
}
