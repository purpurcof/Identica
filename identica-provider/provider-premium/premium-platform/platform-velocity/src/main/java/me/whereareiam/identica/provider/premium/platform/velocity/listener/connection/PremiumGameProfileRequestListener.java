package me.whereareiam.identica.provider.premium.platform.velocity.listener.connection;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.velocitypowered.api.event.player.GameProfileRequestEvent;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.type.AttributeScope;
import me.whereareiam.identica.attributes.ScopedAttributes;
import me.whereareiam.identica.listener.DynamicListener;
import me.whereareiam.identica.model.config.Settings;
import me.whereareiam.identica.provider.premium.PremiumKeys;

import java.time.Duration;
import java.util.UUID;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class PremiumGameProfileRequestListener implements DynamicListener<GameProfileRequestEvent> {
	private final ScopedAttributes scopedAttributes;
	private final Provider<Settings> settingsProvider;

	@Override
	public void onEvent(GameProfileRequestEvent event) {
		UUID profileId = event.getGameProfile().getId();
		if (profileId == null) return;

		String username = event.getUsername();
		if (username == null || username.isBlank()) return;

		long ttlMs = resolveTtlMillis();
		if (ttlMs <= 0) return;

		scopedAttributes.put(AttributeScope.PROFILE_HINT, username, PremiumKeys.PLATFORM_PROFILE_ID, profileId.toString(), ttlMs);
	}

	private long resolveTtlMillis() {
		Duration ttl = settingsProvider.get()
				.getAuthentication()
				.getHandshakeInstructionTtl();

		if (ttl.isZero() || ttl.isNegative())
			return 0;

		return ttl.toMillis();
	}
}
