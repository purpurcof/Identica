package me.whereareiam.identica.model.auth;

import lombok.*;
import me.whereareiam.identica.identity.actor.ConnectionIdentity;
import me.whereareiam.identica.pipeline.ScenarioContext;
import me.whereareiam.identica.pipeline.state.PipelineStateItem;
import me.whereareiam.identica.model.provider.ProviderContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Authentication context containing connection identity and mutable state.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class AuthContext implements ScenarioContext, PipelineStateItem {
	private @Nullable UUID connectionUniqueId;
	private @NotNull ConnectionIdentity identity;
	private @Nullable String intendedServer;

	@Setter
	private @Nullable ProviderContext provider;

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

}
