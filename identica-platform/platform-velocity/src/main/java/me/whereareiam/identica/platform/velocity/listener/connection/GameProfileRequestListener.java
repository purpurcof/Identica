package me.whereareiam.identica.platform.velocity.listener.connection;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.velocitypowered.api.event.player.GameProfileRequestEvent;
import com.velocitypowered.api.util.GameProfile;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.auth.AuthenticationCoordinator;
import me.whereareiam.identica.identity.actor.ConnectionIdentity;
import me.whereareiam.identica.listener.DynamicListener;
import me.whereareiam.identica.model.auth.request.ProfileRequest;
import me.whereareiam.identica.provider.ProviderManager;
import me.whereareiam.identica.provider.profile.ProfileResolution;
import me.whereareiam.identica.provider.profile.ProfileResolveContext;

import java.net.InetSocketAddress;
import java.util.UUID;

@Singleton
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class GameProfileRequestListener implements DynamicListener<GameProfileRequestEvent> {
	private final AuthenticationCoordinator authenticationCoordinator;
	private final ProviderManager providerManager;

	@Override
	public void onEvent(GameProfileRequestEvent event) {
		GameProfile current = event.getGameProfile();
		if (current == null) return;

		String ip = resolveIp(event);

		ConnectionIdentity identity = new ConnectionIdentity(event.getUsername(), ip);

		ProfileResolveContext resolveContext = ProfileResolveContext.builder()
				.identity(identity)
				.build();
		ProfileResolution resolution = providerManager.resolveProfile(resolveContext);
		if (resolution == null) return;

		UUID identicaUuid = authenticationCoordinator.prepareProfile(ProfileRequest.builder()
				.identity(identity)
				.providerId(resolution.getProviderId())
				.providerSubject(resolution.getProviderSubject())
				.build());

		if (identicaUuid == null) return;
		if (current.getId() != null && current.getId().equals(identicaUuid)) return;

		event.setGameProfile(current.withId(identicaUuid));
	}

	private String resolveIp(GameProfileRequestEvent event) {
		InetSocketAddress address = event.getConnection().getRemoteAddress();
		if (address == null || address.getAddress() == null) return null;

		return address.getAddress().getHostAddress();
	}
}
