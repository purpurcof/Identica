package me.whereareiam.identica.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Represents an authenticated session stored in the session cache.
 * <p>
 * Sessions may be partially populated before storage. The session service
 * normalizes missing fields like {@code sessionId}, {@code createdAt},
 * {@code effectiveUsername} during storage.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class Session {
	/**
	 * Session identifier used for lookups; generated when missing during storage.
	 */
	private @Nullable String sessionId;
	/**
	 * Identica account UUID; required for persistence and indexing.
	 */
	private @NotNull UUID uniqueId;

	/**
	 * Provider ID that issued the session (e.g., Premium/Cracked).
	 */
	private @Nullable String providerId;
	/**
	 * Provider subject identifier for lookups.
	 */
	private @Nullable String providerSubject;

	/**
	 * Username observed when the session was created.
	 */
	private @Nullable String originalUsername;
	/**
	 * Effective username after conflict resolution.
	 */
	private @Nullable String effectiveUsername;

	/**
	 * Last known IP address for the session.
	 */
	private @Nullable String ip;
	/**
	 * Creation timestamp (epoch millis).
	 */
	private long createdAt;
}
