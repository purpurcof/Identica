package me.whereareiam.identica.model.auth.request;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import me.whereareiam.identica.identity.actor.ConnectionIdentity;
import me.whereareiam.identica.model.provider.ProviderContext;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Resume request details for pending authentication flows.
 *
 * <p>The resume request should include the latest connection identity so pending
 * flows can re-evaluate IP-sensitive steps. When {@code identity}
 * is omitted, the stored context data is reused.</p>
 */
@Getter
@ToString
@Builder
@SuppressWarnings("unused")
public class ResumeRequest {
	private final @Nullable UUID connectionUniqueId;
	private final @Nullable ConnectionIdentity identity;
	private final @Nullable ProviderContext provider;
	private final @Nullable String intendedServer;

	/**
	 * Returns the connection identity for this resume request.
	 *
	 * @return connection identity or {@code null}
	 */
	public @Nullable ConnectionIdentity getIdentity() {
		return identity;
	}

	/**
	 * Returns the stable identity unique id for this resume request.
	 *
	 * @return identity unique id or {@code null}
	 */
	public @Nullable UUID getIdentityUniqueId() {
		return identity != null ? identity.getUniqueId() : null;
	}

	/**
	 * Returns the username for this resume request.
	 *
	 * @return username or {@code null}
	 */
	public @Nullable String getUsername() {
		return identity != null ? identity.getUsername() : null;
	}

	/**
	 * Returns the IP address for this resume request.
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

	public @Nullable ProviderContext getProvider() {
		return provider;
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
	 * Converts this resume request into a connection request when identity is present.
	 *
	 * @return converted connection request or {@code null}
	 */
	public @Nullable ConnectionRequest toConnectionRequest() {
		if (identity == null) return null;

		return ConnectionRequest.builder()
				.connectionUniqueId(connectionUniqueId)
				.identity(identity)
				.provider(provider)
				.intendedServer(intendedServer)
				.build();
	}
}
