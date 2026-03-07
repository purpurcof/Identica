package me.whereareiam.identica.engine.prepare;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.identity.actor.ConnectionIdentity;
import me.whereareiam.identica.model.provider.ResolvedEntrypoint;
import me.whereareiam.identica.provider.ProviderOperations;
import me.whereareiam.identica.type.provider.ProviderOrigin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ConnectionProviderContextResolver {
	private final ProviderOperations providerOperations;

	public @Nullable ProviderOrigin resolveSource(
			@NotNull ConnectionIdentity identity,
			@Nullable String providerId
	) {
		ConnectionIdentity.Origin origin = identity.getOrigin();
		if (origin == null || origin.getHost().isBlank() || providerId == null || providerId.isBlank())
			return ProviderOrigin.AUTO;

		int port = origin.getPort() != null ? origin.getPort() : -1;
		ResolvedEntrypoint resolved = providerOperations.resolveEntrypoint(origin.getHost(), port);
		if (resolved == null) return ProviderOrigin.AUTO;

		return resolved.getProviderId().equalsIgnoreCase(providerId)
				? ProviderOrigin.ENTRYPOINT
				: ProviderOrigin.AUTO;
	}
}
