package me.whereareiam.identica.adapter;

import me.whereareiam.identica.ConnectionCoordinator;
import me.whereareiam.identica.identity.actor.ConnectionIdentity;
import me.whereareiam.identica.model.auth.request.ProfileRequest;
import me.whereareiam.identica.provider.ProviderOperations;
import me.whereareiam.identica.provider.profile.ProfileResolution;
import me.whereareiam.identica.provider.profile.ProfileResolveContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

/**
 * Base class for resolving Identica resolver rewrites in a platform-agnostic way.
 * Platform adapters should extend this class and provide event-specific handling.
 *
 * <pre>{@code
 * UUID identicaId = profileRewriteAdapter.resolveIdenticaUniqueId(identity);
 * }</pre>
 */
public abstract class ProfileRewriteAdapter {
	private final @NotNull ProviderOperations providerOperations;
	private final @NotNull ConnectionCoordinator connectionCoordinator;

	protected ProfileRewriteAdapter(
			@NotNull ProviderOperations providerOperations,
			@NotNull ConnectionCoordinator connectionCoordinator
	) {
		this.providerOperations = Objects.requireNonNull(providerOperations, "providerOperations");
		this.connectionCoordinator = Objects.requireNonNull(connectionCoordinator, "connectionCoordinator");
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
		ProfileResolution resolution = providerOperations.resolveProfile(resolveContext);
		if (resolution == null)
			return null;

		return connectionCoordinator.prepareProfile(ProfileRequest.builder()
				.identity(identity)
				.providerId(resolution.getProviderId())
				.providerSubject(resolution.getProviderSubject())
				.build());
	}
}
