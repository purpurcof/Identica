package me.whereareiam.identica.platform.velocity.listener.connection;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.proxy.Player;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.pipeline.prepare.PrepareStateStore;
import me.whereareiam.identica.identity.IdentityService;
import me.whereareiam.identica.routing.RoutingStateStore;
import me.whereareiam.identica.verification.VerificationService;
import me.whereareiam.identica.listener.DynamicListener;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class DisconnectListener implements DynamicListener<DisconnectEvent> {
	private final RoutingStateStore routingStateStore;
	private final IdentityService identityService;
	private final PrepareStateStore prepareStateStore;
	private final VerificationService verificationService;

	@Override
	public void onEvent(DisconnectEvent event) {
		Player player = event.getPlayer();
		if (player == null) return;

		routingStateStore.clear(player.getUniqueId());
		identityService.detach(player.getUniqueId());
		prepareStateStore.clear(player.getUniqueId());
		verificationService.cancelPendingEnrollment(player.getUniqueId());
	}
}
