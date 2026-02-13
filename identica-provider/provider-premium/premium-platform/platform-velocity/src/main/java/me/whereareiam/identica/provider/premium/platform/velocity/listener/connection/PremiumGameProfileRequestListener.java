package me.whereareiam.identica.provider.premium.platform.velocity.listener.connection;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.velocitypowered.api.event.player.GameProfileRequestEvent;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.listener.DynamicListener;
import me.whereareiam.identica.model.config.Settings;
import me.whereareiam.identica.pipeline.state.PipelineStateStore;
import me.whereareiam.identica.pipeline.state.PipelineStateReference;
import me.whereareiam.identica.provider.premium.PremiumProfileIdItem;

import java.util.UUID;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class PremiumGameProfileRequestListener implements DynamicListener<GameProfileRequestEvent> {
	private final PipelineStateStore pipelineStateStore;
	private final Provider<Settings> settingsProvider;

	@Override
	public void onEvent(GameProfileRequestEvent event) {
		UUID profileId = event.getGameProfile().getId();
		if (profileId == null) return;

		String username = event.getUsername();
		if (username == null || username.isBlank()) return;
		String ip = resolveIp(event);

		long ttlMs = settingsProvider.get()
				.getConnection()
				.handshakeInstructionTtlMillis();

		PipelineStateReference reference = PipelineStateReference.builder()
				.username(username)
				.ip(ip)
				.build();
		pipelineStateStore.update(reference, ttlMs,
				state -> state.withItem(new PremiumProfileIdItem(profileId.toString()), ttlMs));
	}

	private String resolveIp(GameProfileRequestEvent event) {
		if (event.getConnection().getRemoteAddress() == null)
			return null;

		if (event.getConnection().getRemoteAddress().getAddress() == null)
			return null;

		return event.getConnection().getRemoteAddress().getAddress().getHostAddress();
	}
}
