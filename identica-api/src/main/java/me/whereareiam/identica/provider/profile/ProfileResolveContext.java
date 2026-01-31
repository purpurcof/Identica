package me.whereareiam.identica.provider.profile;

import lombok.Builder;
import lombok.Getter;
import me.whereareiam.identica.identity.actor.ConnectionIdentity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Context passed to profile subject resolvers during profile rewrite.
 */
@Getter
@Builder
@SuppressWarnings("unused")
public class ProfileResolveContext {
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
