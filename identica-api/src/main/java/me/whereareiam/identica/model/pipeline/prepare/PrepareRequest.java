package me.whereareiam.identica.model.pipeline.prepare;

import lombok.*;
import me.whereareiam.identica.identity.actor.ConnectionIdentity;
import me.whereareiam.identica.model.identity.IdentityReference;
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
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PrepareRequest {
	@Builder.Default
	private final @NotNull IdentityReference identityReference = new IdentityReference();
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

	public @Nullable UUID getConnectionUniqueId() {
		if (identityReference.getConnectionUniqueId() != null)
			return identityReference.getConnectionUniqueId();

		return identity.getConnectionUniqueId();
	}

	public @Nullable UUID getAccountUniqueId() {
		if (identityReference.getAccountUniqueId() != null)
			return identityReference.getAccountUniqueId();

		return identity.getAccountUniqueId();
	}

	public static class PrepareRequestBuilder {
		private final IdentityReference identityReference = new IdentityReference();

		public @NotNull PrepareRequestBuilder connectionUniqueId(@Nullable java.util.UUID connectionUniqueId) {
			identityReference.setConnectionUniqueId(connectionUniqueId);
			return this;
		}

		public @NotNull PrepareRequestBuilder accountUniqueId(@Nullable java.util.UUID accountUniqueId) {
			identityReference.setAccountUniqueId(accountUniqueId);
			return this;
		}

		public @NotNull PrepareRequest build() {
			return new PrepareRequest(identityReference, connectionKey, stage, identity);
		}
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
