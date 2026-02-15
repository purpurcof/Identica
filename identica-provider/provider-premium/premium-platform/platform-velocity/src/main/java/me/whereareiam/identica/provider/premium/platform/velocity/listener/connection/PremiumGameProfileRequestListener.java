package me.whereareiam.identica.provider.premium.platform.velocity.listener.connection;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.velocitypowered.api.event.player.GameProfileRequestEvent;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.listener.DynamicListener;
import me.whereareiam.identica.provider.premium.profile.PremiumProfileStore;
import me.whereareiam.identica.util.UniqueIdGenerator;

import java.util.UUID;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class PremiumGameProfileRequestListener implements DynamicListener<GameProfileRequestEvent> {
	private final PremiumProfileStore profileStore;

	@Override
	public void onEvent(GameProfileRequestEvent event) {
		UUID profileId = event.getGameProfile().getId();
		if (profileId == null) return;

		String username = event.getUsername();
		if (username == null || username.isBlank()) return;
		String ip = resolveIp(event);

		UUID offlineUuid = UniqueIdGenerator.offlinePlayerUniqueId(username);
		if (offlineUuid != null && !profileId.equals(offlineUuid)) {
			profileStore.clearAttempt(username, ip);
		}

		profileStore.save(username, ip, profileId.toString());
	}

	private String resolveIp(GameProfileRequestEvent event) {
		if (event.getConnection().getRemoteAddress() == null) return null;
		if (event.getConnection().getRemoteAddress().getAddress() == null) return null;

		return event.getConnection().getRemoteAddress().getAddress().getHostAddress();
	}
}
