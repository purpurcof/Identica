package me.whereareiam.identica.model.auth;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import me.whereareiam.identica.identity.actor.OfflineIdentity;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Connection-scoped identity details used during authentication flows.
 * <p>
 * Example:
 * <pre>{@code
 * ConnectionInfo info = ConnectionInfo.builder()
 *     .identity(new OfflineIdentity("Steve", "127.0.0.1"))
 *     .onlineMode(true)
 *     .build();
 * }</pre>
 */
@Getter
@ToString
@Builder
public class ConnectionInfo {
	private final @Nullable OfflineIdentity identity;
	private final boolean onlineMode;

	/**
	 * Returns the Identica unique id from the nested identity.
	 *
	 * @return unique id or {@code null}
	 */
	public @Nullable UUID getIdenticaUniqueId() {
		return identity != null ? identity.getUniqueId() : null;
	}

	/**
	 * Updates the Identica unique id on the nested identity.
	 *
	 * @param identicaUniqueId unique id to assign
	 */
	public void setIdenticaUniqueId(@Nullable UUID identicaUniqueId) {
		if (identity == null) return;
		identity.setUniqueId(identicaUniqueId);
	}

	/**
	 * Returns the username from the nested identity.
	 *
	 * @return username or {@code null}
	 */
	public @Nullable String getUsername() {
		return identity != null ? identity.getUsername() : null;
	}

	/**
	 * Returns the IP address from the nested identity.
	 *
	 * @return IP address or {@code null}
	 */
	public @Nullable String getIp() {
		return identity != null ? identity.getIp() : null;
	}
}
