package me.whereareiam.identica.model.auth.handshake;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import me.whereareiam.identica.handshake.HandshakeAttributeKey;
import me.whereareiam.identica.identity.actor.ConnectionIdentity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Stored handshake instruction for a pending connection.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@SuppressWarnings("unused")
public class HandshakeInstruction {
	private @NotNull ConnectionIdentity identity;
	private @Nullable Map<String, String> attributes = new HashMap<>();
	private long expiresAt;

	public HandshakeInstruction(@NotNull ConnectionIdentity identity, long expiresAt) {
		this.identity = identity;
		this.expiresAt = expiresAt;
	}

	/**
	 * Creates a new handshake instruction with a TTL.
	 *
	 * @param identity connection identity
	 * @param ttlMillis time to live in milliseconds
	 * @return instruction instance
	 */
	public static @NotNull HandshakeInstruction create(
			@NotNull ConnectionIdentity identity,
			long ttlMillis
	) {
		long expiresAt = System.currentTimeMillis() + ttlMillis;
		return new HandshakeInstruction(identity, expiresAt);
	}

	/**
	 * Sets a typed attribute value on this instruction.
	 *
	 * @param key attribute key
	 * @param value attribute value
	 * @param <T> attribute type
	 */
	public <T> void setAttribute(@NotNull HandshakeAttributeKey<T> key, @NotNull T value) {
		Map<String, String> resolved = ensureAttributes();
		resolved.put(key.id(), key.encoder().apply(value));
	}

	/**
	 * Resolves a typed attribute value.
	 *
	 * @param key attribute key
	 * @param <T> attribute type
	 * @return resolved value, if present
	 */
	public <T> @NotNull Optional<T> getAttribute(@NotNull HandshakeAttributeKey<T> key) {
		Map<String, String> resolved = ensureAttributes();
		String payload = resolved.get(key.id());

		if (payload == null) return Optional.empty();
		return Optional.ofNullable(key.decoder().apply(payload));
	}

	/**
	 * Removes a typed attribute.
	 *
	 * @param key attribute key
	 * @return {@code true} if removed
	 */
	public boolean removeAttribute(@NotNull HandshakeAttributeKey<?> key) {
		Map<String, String> resolved = ensureAttributes();
		return resolved.remove(key.id()) != null;
	}

	private @NotNull Map<String, String> ensureAttributes() {
		if (attributes == null) attributes = new HashMap<>();
		return attributes;
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
