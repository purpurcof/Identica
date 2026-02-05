package me.whereareiam.identica.model.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import me.whereareiam.identica.identity.actor.ConnectionIdentity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Authentication context containing connection identity and mutable state.
 */
@Getter
@AllArgsConstructor
@Builder(toBuilder = true)
@SuppressWarnings("unused")
public class AuthContext {
	private final @Nullable UUID connectionUniqueId;
	private final @NotNull ConnectionIdentity identity;
	private final @Nullable String intendedServer;

	@Setter
	private @Nullable AuthContext.Provider provider;

	/**
	 * Returns the connection identity attached to this context.
	 *
	 * @return connection identity
	 */
	public @NotNull ConnectionIdentity getIdentity() {
		return identity;
	}

	/**
	 * Returns the Identica unique id assigned to this connection.
	 *
	 * @return unique id or {@code null}
	 */
	public @Nullable UUID getIdenticaUniqueId() {
		return identity.getUniqueId();
	}

	/**
	 * Sets the Identica unique id for this connection.
	 *
	 * @param identicaUniqueId unique id to assign
	 */
	public void setIdenticaUniqueId(@Nullable UUID identicaUniqueId) {
		identity.setUniqueId(identicaUniqueId);
	}

	/**
	 * Returns the username for this connection.
	 *
	 * @return username or {@code null}
	 */
	public @Nullable String getUsername() {
		return identity.getUsername();
	}

	/**
	 * Returns the IP address for this connection.
	 *
	 * @return IP address or {@code null}
	 */
	public @Nullable String getIp() {
		return identity.getIp();
	}

	/**
	 * Provider claim captured after authentication.
	 */
	@Getter
	@Setter
	@ToString
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder(toBuilder = true)
	public static class Provider {
		private @Nullable String providerId;
		private @Nullable String providerSubject;
		private @NotNull String providerUsername;
	}

}
