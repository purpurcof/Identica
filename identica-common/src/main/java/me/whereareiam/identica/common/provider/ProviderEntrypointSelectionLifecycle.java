package me.whereareiam.identica.common.provider;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.whereareiam.identica.event.EventListener;
import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.event.base.IdenticEvent;
import me.whereareiam.identica.event.pipeline.attempt.ScenarioContextBuiltEvent;
import me.whereareiam.identica.identity.actor.ConnectionIdentity;
import me.whereareiam.identica.model.provider.ProviderContext;
import me.whereareiam.identica.model.provider.ResolvedEntrypoint;
import me.whereareiam.identica.pipeline.ScenarioContext;
import me.whereareiam.identica.provider.ProviderOperations;
import me.whereareiam.identica.type.event.EventOrder;
import me.whereareiam.identica.type.provider.ProviderOrigin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Singleton
public class ProviderEntrypointSelectionLifecycle implements EventListener {
	private final ProviderOperations providerOperations;

	@Inject
	public ProviderEntrypointSelectionLifecycle(
			@NotNull ProviderOperations providerOperations,
			@NotNull EventManager eventManager
	) {
		this.providerOperations = providerOperations;
		eventManager.register(this);
	}

	@IdenticEvent(EventOrder.HIGHEST)
	public void onContextBuilt(@NotNull ScenarioContextBuiltEvent event) {
		ScenarioContext context = event.getContext();
		if (context == null) return;

		ProviderContext provider = context.getProvider();
		if (provider != null && isPresent(provider.getProviderId())) return;

		ConnectionIdentity.Origin origin = context.getIdentity().getOrigin();
		if (origin == null || !isPresent(origin.host())) return;

		Integer port = origin.port();
		int resolvedPort = port != null ? port : -1;

		ResolvedEntrypoint resolved = providerOperations.resolveEntrypoint(origin.host(), resolvedPort);
		if (resolved == null) return;

		String username = context.getUsername();
		String providerUsername = username != null ? username : "";

		if (provider == null) {
			context.setProvider(ProviderContext.builder()
					.providerId(resolved.getProviderId())
					.providerUsername(providerUsername)
					.source(ProviderOrigin.ENTRYPOINT)
					.build());
			return;
		}

		if (!isPresent(provider.getProviderId()))
			provider.setProviderId(resolved.getProviderId());
		if (!isPresent(provider.getProviderUsername()))
			provider.setProviderUsername(providerUsername);

		provider.setSource(ProviderOrigin.ENTRYPOINT);
	}

	private boolean isPresent(@Nullable String value) {
		return value != null && !value.isBlank();
	}
}
