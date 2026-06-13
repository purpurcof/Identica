package me.whereareiam.identica.provider.subject;

import lombok.Builder;
import lombok.Getter;
import me.whereareiam.identica.identity.actor.ConnectionIdentity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Context passed to subject resolvers during provider-subject derivation.
 */
@Getter
@Builder
@SuppressWarnings("unused")
public class SubjectResolveContext {
	private final @NotNull ConnectionIdentity identity;

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
