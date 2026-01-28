package me.whereareiam.identica.model.auth.request;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import me.whereareiam.identica.identity.actor.OfflineIdentity;
import me.whereareiam.identica.model.auth.ConnectionInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Login request details used to build the authentication context.
 */
@Getter
@ToString
@Builder
@SuppressWarnings("unused")
public class LoginRequest {
	private final @Nullable UUID connectionUniqueId;
	private final @NotNull ConnectionInfo connectionInfo;
	private final @Nullable String intendedServer;

	/**
	 * Returns the offline identity for this login request.
	 *
	 * @return offline identity or {@code null}
	 */
	public @Nullable OfflineIdentity getIdentity() {
		return connectionInfo.getIdentity();
	}

	/**
	 * Returns whether the connection is in online mode.
	 *
	 * @return {@code true} when online mode is enabled
	 */
	public boolean isOnlineMode() {
		return connectionInfo.isOnlineMode();
	}
}
