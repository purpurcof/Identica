package me.whereareiam.identica.model.auth.request;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import me.whereareiam.identica.identity.actor.ConnectionIdentity;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Advance request details for in-session authentication journeys.
 *
 * <p>The advance request should include the latest connection identity so pending
 * journeyModes can re-evaluate IP-sensitive steps. When {@code identity}
 * is omitted, the stored context data is reused.</p>
 */
@Getter
@ToString
@Builder
@SuppressWarnings("unused")
public class AdvanceRequest {
	private final @Nullable UUID connectionUniqueId;
	private final @Nullable ConnectionIdentity identity;
	private final @Nullable String intendedServer;

	/**
	 * Returns the connection identity for this advance request.
	 *
	 * @return connection identity or {@code null}
	 */
	public @Nullable ConnectionIdentity getIdentity() {
		return identity;
	}

	/**
	 * Returns the stable identity unique id for this advance request.
	 *
	 * @return identity unique id or {@code null}
	 */
	public @Nullable UUID getIdentityUniqueId() {
		return identity != null ? identity.getUniqueId() : null;
	}

	/**
	 * Returns the username for this advance request.
	 *
	 * @return username or {@code null}
	 */
	public @Nullable String getUsername() {
		return identity != null ? identity.getUsername() : null;
	}

	/**
	 * Returns the IP address for this advance request.
	 *
	 * @return IP address or {@code null}
	 */
	public @Nullable String getIp() {
		return identity != null ? identity.getIp() : null;
	}

	/**
	 * Returns whether this request supplies connection identity.
	 *
	 * @return {@code true} if connection identity is present
	 */
	public boolean hasIdentity() {
		return identity != null;
	}

	/**
	 * Returns the connection identity for this request.
	 *
	 * @return connection identity or {@code null}
	 */
	public @Nullable ConnectionIdentity getIdentityInfo() {
		return identity;
	}

	/**
	 * Returns the intended server name for this request.
	 *
	 * @return server name or {@code null}
	 */
	public @Nullable String getIntendedServer() {
		return intendedServer;
	}

	/**
	 * Converts this advance request into a connection request when identity is present.
	 *
	 * @return converted connection request or {@code null}
	 */
	public @Nullable ConnectionRequest toConnectionRequest() {
		if (identity == null)
			return null;

		return ConnectionRequest.builder()
				.connectionUniqueId(connectionUniqueId)
				.identity(identity)
				.intendedServer(intendedServer)
				.build();
	}
}
