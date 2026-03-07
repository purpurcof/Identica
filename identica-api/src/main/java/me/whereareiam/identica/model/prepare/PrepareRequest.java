package me.whereareiam.identica.model.prepare;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import me.whereareiam.identica.identity.actor.ConnectionIdentity;
import me.whereareiam.identica.type.PrepareStage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Generic connection preparation request.
 *
 * <p>The platform can invoke preparation in multiple stages while reusing the
 * previous decision as more connection details become available.</p>
 */
@Getter
@ToString
@Builder
public class PrepareRequest {
	private final @Nullable UUID connectionUniqueId;
	private final @Nullable String connectionKey;
	private final @NotNull PrepareStage stage;
	private final @NotNull ConnectionIdentity identity;

	/**
	 * Returns the normalized preparation stage.
	 *
	 * @return resolved stage
	 */
	public @NotNull PrepareStage getStage() {
		return PrepareStage.resolve(stage);
	}

	/**
	 * Returns the canonical staged-prepare connection key.
	 *
	 * <p>If an explicit key was provided, it is used as-is. Otherwise the key is
	 * derived from the connection identity so the prepare pipeline can restore
	 * and persist staged state consistently without requiring platforms to build
	 * the key themselves.</p>
	 *
	 * @return resolved connection key
	 */
	public @Nullable String getConnectionKey() {
		if (connectionKey != null && !connectionKey.isBlank())
			return connectionKey;

		ConnectionIdentity.Origin origin = identity.getOrigin();
		String host = origin != null ? origin.getHost() : "";
		Integer port = origin != null ? origin.getPort() : null;
		return String.join("|",
				identity.getUsername(),
				identity.getIp() == null ? "" : identity.getIp(),
				host,
				port == null ? "" : Integer.toString(port));
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
