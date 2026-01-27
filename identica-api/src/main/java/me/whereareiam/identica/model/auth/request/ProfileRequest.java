package me.whereareiam.identica.model.auth.request;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import me.whereareiam.identica.model.auth.ConnectionInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Profile rewrite request details for connection-specific UUID resolution.
 *
 * <p>Provider id and provider subject must be supplied to resolve an existing
 * Identica UUID and avoid duplicate accounts.</p>
 */
@Getter
@ToString
@Builder
public class ProfileRequest {
	private final @NotNull ConnectionInfo connectionInfo;
	private final @NotNull String providerId;
	private final @NotNull String providerSubject;
	private final @Nullable String profileUniqueId;

	/**
	 * Returns the provider id used for UUID rewriting.
	 *
	 * @return provider id
	 */
	public @NotNull String getProviderId() {
		return providerId;
	}

	/**
	 * Returns the provider subject used for UUID rewriting.
	 *
	 * @return provider subject
	 */
	public @NotNull String getProviderSubject() {
		return providerSubject;
	}

	/**
	 * Returns the username for this connection.
	 *
	 * @return username or {@code null}
	 */
	public @Nullable String getUsername() {
		return connectionInfo.getUsername();
	}

	/**
	 * Returns the IP address for this connection.
	 *
	 * @return IP address or {@code null}
	 */
	public @Nullable String getIp() {
		return connectionInfo.getIp();
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
