package me.whereareiam.identica.model.auth.handshake;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import me.whereareiam.identica.actor.OfflineIdentity;
import me.whereareiam.identica.type.HandshakeMode;
import org.jetbrains.annotations.NotNull;

/**
 * Stored handshake instruction for a pending connection.
 */
@Getter
@ToString
@RequiredArgsConstructor
public class HandshakeInstruction {
	private final @NotNull OfflineIdentity identity;
	private final @NotNull HandshakeMode mode;
	private final long expiresAt;

	/**
	 * Creates a new handshake instruction with a TTL.
	 *
	 * @param identity connection identity
	 * @param mode requested handshake mode
	 * @param ttlMillis time to live in milliseconds
	 * @return instruction instance
	 */
	public static @NotNull HandshakeInstruction create(
			@NotNull OfflineIdentity identity,
			@NotNull HandshakeMode mode,
			long ttlMillis
	) {
		long expiresAt = System.currentTimeMillis() + ttlMillis;
		return new HandshakeInstruction(identity, mode, expiresAt);
	}

	/**
	 * Checks whether this instruction is expired.
	 *
	 * @param now current time in epoch millis
	 * @return {@code true} when expired
	 */
	public boolean isExpired(long now) {
		return expiresAt <= now;
	}
}
