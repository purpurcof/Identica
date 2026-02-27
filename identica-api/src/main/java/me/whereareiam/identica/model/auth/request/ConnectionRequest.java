package me.whereareiam.identica.model.auth.request;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import me.whereareiam.identica.identity.actor.ConnectionIdentity;
import me.whereareiam.identica.model.provider.ProviderContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Connection request details used to build the connection context.
 */
@Getter
@ToString
@Builder
@SuppressWarnings("unused")
public class ConnectionRequest {
	private final @Nullable UUID connectionUniqueId;
	private final @NotNull ConnectionIdentity identity;
	private final @Nullable String intendedServer;
	/**
	 * Provider context selected for this connection, if known.
	 */
	private final @Nullable ProviderContext provider;

	/**
	 * Returns the connection identity for this connection request.
	 *
	 * @return connection identity
	 */
	public @NotNull ConnectionIdentity getIdentity() {
		return identity;
	}

	/**
	 * Returns the username from the connection identity.
	 *
	 * @return username or {@code null}
	 */
	public @Nullable String getUsername() {
		return identity.getUsername();
	}

	/**
	 * Returns the IP address from the connection identity.
	 *
	 * @return IP address or {@code null}
	 */
	public @Nullable String getIp() {
		return identity.getIp();
	}
}
