package me.whereareiam.identica.model.pipeline.prepare;

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
