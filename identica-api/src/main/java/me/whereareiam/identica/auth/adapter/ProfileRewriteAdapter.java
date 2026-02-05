package me.whereareiam.identica.auth.adapter;

import me.whereareiam.identica.auth.AuthenticationCoordinator;
import me.whereareiam.identica.identity.actor.ConnectionIdentity;
import me.whereareiam.identica.model.auth.request.ProfileRequest;
import me.whereareiam.identica.provider.ProviderManager;
import me.whereareiam.identica.provider.profile.ProfileResolution;
import me.whereareiam.identica.provider.profile.ProfileResolveContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

/**
 * Base class for resolving Identica profile rewrites in a platform-agnostic way.
 * Platform adapters should extend this class and provide event-specific handling.
 *
 * <pre>{@code
 * UUID identicaId = profileRewriteAdapter.resolveIdenticaUniqueId(identity);
 * }</pre>
 */
public abstract class ProfileRewriteAdapter {
	private final @NotNull ProviderManager providerManager;
	private final @NotNull AuthenticationCoordinator authenticationCoordinator;

	protected ProfileRewriteAdapter(
			@NotNull ProviderManager providerManager,
			@NotNull AuthenticationCoordinator authenticationCoordinator
	) {
		this.providerManager = Objects.requireNonNull(providerManager, "providerManager");
		this.authenticationCoordinator = Objects.requireNonNull(authenticationCoordinator, "authenticationCoordinator");
	}

	/**
	 * Resolves or prepares the Identica unique id for the provided connection identity.
	 *
	 * @param identity connection identity
	 * @return resolved Identica unique id or {@code null} when unavailable
	 */
	protected @Nullable UUID resolveIdenticaUniqueId(@NotNull ConnectionIdentity identity) {
		ProfileResolveContext resolveContext = ProfileResolveContext.builder()
				.identity(identity)
				.build();
		ProfileResolution resolution = providerManager.resolveProfile(resolveContext);
		if (resolution == null)
			return null;

		return authenticationCoordinator.prepareProfile(ProfileRequest.builder()
				.identity(identity)
				.providerId(resolution.getProviderId())
				.providerSubject(resolution.getProviderSubject())
				.build());
	}
}
