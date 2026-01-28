package me.whereareiam.identica.identity.actor;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Lightweight identity representation for offline/authentication flows.
 */
@Getter
@Setter
@AllArgsConstructor
public class OfflineIdentity {
	private @Nullable UUID uniqueId;
	private final @NotNull String username;
	private final @Nullable String ip;

	/**
	 * Creates an offline identity without a preassigned unique id.
	 *
	 * @param username username for the connection
	 * @param ip optional IP address
	 */
	public OfflineIdentity(@NotNull String username, @Nullable String ip) {
		this(null, username, ip);
	}
}
