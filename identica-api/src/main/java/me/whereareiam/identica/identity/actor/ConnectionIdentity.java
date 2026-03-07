package me.whereareiam.identica.identity.actor;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
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
	private @Nullable UUID observedUniqueId;
	private @NotNull String username;
	private @Nullable String ip;
	/**
	 * Address the client used to connect, if available.
	 */
	private @Nullable Origin origin;

	/**
	 * Origin address used by the client to connect.
	 *
	 * @param host host name used by the client
	 * @param port port used by the client, if provided
	 */
	@Getter
	@Setter
	@NoArgsConstructor
	@AllArgsConstructor
	@EqualsAndHashCode
	@ToString
	public static class Origin {
		private @NotNull String host;
		private @Nullable Integer port;
	}

	/**
	 * Creates a connection identity without a preassigned unique id.
	 *
	 * @param username username for the connection
	 * @param ip optional IP address
	 */
	public ConnectionIdentity(@NotNull String username, @Nullable String ip) {
		this(null, null, username, ip, null);
	}

	/**
	 * Creates a connection identity with a preassigned unique id.
	 *
	 * @param uniqueId unique id for the connection
	 * @param username username for the connection
	 * @param ip optional IP address
	 */
	public ConnectionIdentity(@Nullable UUID uniqueId, @NotNull String username, @Nullable String ip) {
		this(uniqueId, null, username, ip, null);
	}

	public ConnectionIdentity(
			@Nullable UUID uniqueId,
			@Nullable UUID observedUniqueId,
			@NotNull String username,
			@Nullable String ip
	) {
		this(uniqueId, observedUniqueId, username, ip, null);
	}
}
