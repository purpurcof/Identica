package me.whereareiam.identica.platform.velocity.listener.connection;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.proxy.Player;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.presence.PresenceService;
import me.whereareiam.identica.routing.RoutingStateStore;
import me.whereareiam.identica.listener.DynamicListener;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class DisconnectListener implements DynamicListener<DisconnectEvent> {
	private final RoutingStateStore routingStateStore;
	private final PresenceService presenceService;

	@Override
	public void onEvent(DisconnectEvent event) {
		Player player = event.getPlayer();
		if (player == null) return;

		routingStateStore.clear(player.getUniqueId());
		presenceService.detach(player.getUniqueId());
	}
}
