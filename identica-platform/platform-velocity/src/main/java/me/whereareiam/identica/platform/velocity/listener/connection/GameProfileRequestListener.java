package me.whereareiam.identica.platform.velocity.listener.connection;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.velocitypowered.api.event.player.GameProfileRequestEvent;
import com.velocitypowered.api.util.GameProfile;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.auth.AuthCoordinator;
import me.whereareiam.identica.actor.OfflineIdentity;
import me.whereareiam.identica.listener.DynamicListener;
import me.whereareiam.identica.model.auth.ConnectionInfo;
import me.whereareiam.identica.model.auth.request.ProfileRequest;
import me.whereareiam.identica.util.UniqueIdGenerator;
import me.whereareiam.identica.loader.ProviderManager;
import me.whereareiam.identica.model.provider.InternalProvider;
import me.whereareiam.identica.type.provider.ProviderCapability;

import java.net.InetSocketAddress;
import java.util.UUID;

@Singleton
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class GameProfileRequestListener implements DynamicListener<GameProfileRequestEvent> {
	private final AuthCoordinator authCoordinator;
	private final ProviderManager providerManager;

	@Override
	public void onEvent(GameProfileRequestEvent event) {
		GameProfile current = event.getGameProfile();
		if (current == null) return;

		String profileUniqueId = current.getId() != null ? current.getId().toString() : null;
		String providerId = resolveProviderId(event.isOnlineMode());
		if (providerId == null) return;
		String providerSubject = resolveProviderSubject(event, profileUniqueId);
		if (providerSubject == null) return;
		String ip = resolveIp(event);

		UUID identicaUuid = authCoordinator.prepareProfile(ProfileRequest.builder()
				.connectionInfo(ConnectionInfo.builder()
						.identity(new OfflineIdentity(event.getUsername(), ip))
						.onlineMode(event.isOnlineMode())
						.build())
				.providerId(providerId)
				.providerSubject(providerSubject)
				.profileUniqueId(profileUniqueId)
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

	private String resolveProviderSubject(GameProfileRequestEvent event, String profileUniqueId) {
		if (profileUniqueId != null && !profileUniqueId.isBlank())
			return profileUniqueId.trim();
		if (event.isOnlineMode()) return null;

		String username = event.getUsername();
		if (username == null || username.isBlank()) return null;

		UUID offlineUuid = UniqueIdGenerator.offlinePlayerUniqueId(username);
		return offlineUuid != null ? offlineUuid.toString() : null;
	}

	private String resolveProviderId(boolean onlineMode) {
		ProviderCapability capability = onlineMode
				? ProviderCapability.ONLINE_MODE
				: ProviderCapability.OFFLINE_MODE;
		InternalProvider provider = providerManager.findProvider(capability);
		if (provider == null || provider.getDescriptor() == null) return null;

		String id = provider.getDescriptor().getId();
		return id != null && !id.isBlank() ? id : null;
	}

}
