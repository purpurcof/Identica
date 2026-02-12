package me.whereareiam.identica.platform.velocity.listener.connection.server;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.event.routing.RoutingTargetMissingEvent;
import me.whereareiam.identica.listener.DynamicListener;
import me.whereareiam.identica.model.RoutingTarget;
import me.whereareiam.identica.routing.RoutingStateStore;
import me.whereareiam.identica.type.RoutingTargetType;

import java.util.Optional;
import java.util.UUID;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class PlayerChooseInitialServerListener implements DynamicListener<PlayerChooseInitialServerEvent> {
	private final ProxyServer proxyServer;
	private final RoutingStateStore routingStateStore;
	private final EventManager eventManager;

	@Override
	public void onEvent(PlayerChooseInitialServerEvent event) {
		UUID connectionId = event.getPlayer().getUniqueId();
		RoutingTarget target = routingStateStore.peek(connectionId).orElse(null);
		if (target == null) return;
		if (target.getServer() == null || target.getServer().isBlank()) return;

		Optional<RegisteredServer> server = proxyServer.getServer(target.getServer());
		if (server.isEmpty()) {
			RoutingTargetMissingEvent missingEvent = new RoutingTargetMissingEvent(
					connectionId,
					event.getPlayer().getUsername(),
					target
			);
			eventManager.call(missingEvent);
			if (missingEvent.isDisconnect() && missingEvent.getMessage() != null) {
				event.getPlayer().disconnect(missingEvent.getMessage());
			}
			return;
		}

		if (target.getType() == RoutingTargetType.COMPLETED)
			routingStateStore.consume(connectionId);
		event.setInitialServer(server.get());
	}
}
