package me.whereareiam.identica.provider.premium.platform.velocity.listener.connection;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.velocitypowered.api.event.player.GameProfileRequestEvent;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.flow.FlowRefeference;
import me.whereareiam.identica.flow.FlowTransit;
import me.whereareiam.identica.listener.DynamicListener;
import me.whereareiam.identica.model.config.Settings;
import me.whereareiam.identica.provider.premium.PremiumFlowSignals;

import java.time.Duration;
import java.util.UUID;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class PremiumGameProfileRequestListener implements DynamicListener<GameProfileRequestEvent> {
	private final FlowTransit flowTransit;
	private final Provider<Settings> settingsProvider;

	@Override
	public void onEvent(GameProfileRequestEvent event) {
		UUID profileId = event.getGameProfile().getId();
		if (profileId == null) return;

		String username = event.getUsername();
		if (username == null || username.isBlank()) return;
		String ip = resolveIp(event);

		long ttlMs = resolveTtlMillis();
		if (ttlMs <= 0) return;

		flowTransit.scope(FlowRefeference.builder()
						.username(username)
						.ip(ip)
						.build())
				.put(PremiumFlowSignals.PLATFORM_PROFILE_ID, profileId.toString())
				.ttlMillis(ttlMs);
	}

	private long resolveTtlMillis() {
		Duration ttl = settingsProvider.get()
				.getAuthentication()
				.getHandshakeInstructionTtl();

		if (ttl.isZero() || ttl.isNegative())
			return 0;

		return ttl.toMillis();
	}

	private String resolveIp(GameProfileRequestEvent event) {
		if (event.getConnection().getRemoteAddress() == null)
			return null;

		if (event.getConnection().getRemoteAddress().getAddress() == null)
			return null;

		return event.getConnection().getRemoteAddress().getAddress().getHostAddress();
	}
}
