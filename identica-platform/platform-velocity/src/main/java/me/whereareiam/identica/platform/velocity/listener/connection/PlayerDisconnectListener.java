package me.whereareiam.identica.platform.velocity.listener.connection;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.proxy.Player;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.auth.AuthCoordinator;
import me.whereareiam.identica.listener.DynamicListener;
import me.whereareiam.identica.routing.RoutingStateStore;
import me.whereareiam.identica.registry.IdentityRegistry;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class PlayerDisconnectListener implements DynamicListener<DisconnectEvent> {
	private final AuthCoordinator authCoordinator;
	private final RoutingStateStore routingStateStore;
	private final IdentityRegistry identityRegistry;

	@Override
	public void onEvent(DisconnectEvent event) {
		Player player = event.getPlayer();
		if (player == null) return;

		authCoordinator.clearPending(player.getUniqueId());
		routingStateStore.clear(player.getUniqueId());
		identityRegistry.detachOnline(player.getUniqueId());
	}
}
