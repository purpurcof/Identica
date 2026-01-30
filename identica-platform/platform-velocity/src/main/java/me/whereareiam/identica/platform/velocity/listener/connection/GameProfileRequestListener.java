package me.whereareiam.identica.platform.velocity.listener.connection;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.velocitypowered.api.event.player.GameProfileRequestEvent;
import com.velocitypowered.api.util.GameProfile;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.auth.AuthenticationCoordinator;
import me.whereareiam.identica.provider.eligibility.ProviderEligibilityService;
import me.whereareiam.identica.identity.actor.OfflineIdentity;
import me.whereareiam.identica.listener.DynamicListener;
import me.whereareiam.identica.model.auth.AuthContext;
import me.whereareiam.identica.model.auth.ConnectionInfo;
import me.whereareiam.identica.model.auth.request.ProfileRequest;
import me.whereareiam.identica.model.config.Settings;
import me.whereareiam.identica.util.UniqueIdGenerator;
import me.whereareiam.identica.provider.ProviderManager;
import me.whereareiam.identica.model.provider.InternalProvider;
import me.whereareiam.identica.type.provider.ProviderCapability;
import me.whereareiam.identica.type.step.AuthFlowType;

import java.net.InetSocketAddress;
import java.util.UUID;
import com.google.inject.Provider;

@Singleton
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class GameProfileRequestListener implements DynamicListener<GameProfileRequestEvent> {
	private final AuthenticationCoordinator authenticationCoordinator;
	private final ProviderManager providerManager;
	private final ProviderEligibilityService eligibilityService;
	private final Provider<Settings> settingsProvider;

	@Override
	public void onEvent(GameProfileRequestEvent event) {
		GameProfile current = event.getGameProfile();
		if (current == null) return;

		String profileUniqueId = current.getId() != null ? current.getId().toString() : null;
		String providerId = resolveProviderId(event);
		if (providerId == null) return;
		String providerSubject = resolveProviderSubject(event, profileUniqueId);
		if (providerSubject == null) return;
		String ip = resolveIp(event);

		UUID identicaUuid = authenticationCoordinator.prepareProfile(ProfileRequest.builder()
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

	private String resolveProviderId(GameProfileRequestEvent event) {
		boolean onlineMode = event.isOnlineMode();
		ProviderCapability capability = onlineMode
				? ProviderCapability.ONLINE_MODE
				: ProviderCapability.OFFLINE_MODE;

		AuthContext context = AuthContext.builder()
				.connectionInfo(ConnectionInfo.builder()
						.identity(new OfflineIdentity(event.getUsername(), resolveIp(event)))
						.onlineMode(onlineMode)
						.build())
				.build();

		AuthFlowType preferred = settingsProvider.get().getAuthentication().getFlow();
		String resolved = resolveProviderIdForFlow(context, capability, preferred);
		if (resolved != null) return resolved;

		AuthFlowType fallback = preferred == AuthFlowType.SEAMLESS
				? AuthFlowType.INTERACTIVE
				: AuthFlowType.SEAMLESS;
		resolved = resolveProviderIdForFlow(context, capability, fallback);
		if (resolved != null) return resolved;

		InternalProvider provider = providerManager.findProvider(capability);
		if (provider == null || provider.getDescriptor() == null) return null;

		String id = provider.getDescriptor().getId();
		return !id.isBlank() ? id : null;
	}

	private String resolveProviderIdForFlow(
			AuthContext context,
			ProviderCapability capability,
			AuthFlowType flow
	) {
		for (InternalProvider provider : eligibilityService.eligibleProviders(context, flow)) {
			if (provider == null || provider.getDescriptor() == null) continue;
			if (!provider.getDescriptor().hasCapability(capability)) continue;

			String id = provider.getDescriptor().getId();
			if (!id.isBlank())
				return id;
		}

		return null;
	}
}
