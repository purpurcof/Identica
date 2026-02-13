package me.whereareiam.identica.identity.actor;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Lightweight identity representation for connection/authentication flows.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConnectionIdentity {
	private @Nullable UUID uniqueId;
	private @NotNull String username;
	private @Nullable String ip;

	/**
	 * Creates a connection identity without a preassigned unique id.
	 *
	 * @param username username for the connection
	 * @param ip optional IP address
	 */
	public ConnectionIdentity(@NotNull String username, @Nullable String ip) {
		this(null, username, ip);
	}
}
