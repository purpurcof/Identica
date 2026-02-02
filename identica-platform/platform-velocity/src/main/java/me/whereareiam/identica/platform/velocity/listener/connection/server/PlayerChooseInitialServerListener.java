package me.whereareiam.identica.platform.velocity.listener.connection.server;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.connection.ConnectionStateRegistry;
import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.event.routing.RoutingTargetMissingEvent;
import me.whereareiam.identica.listener.DynamicListener;
import me.whereareiam.identica.model.RoutingTarget;
import me.whereareiam.identica.model.connection.ConnectionState;
import me.whereareiam.identica.type.RoutingTargetType;

import java.util.Optional;
import java.util.UUID;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class PlayerChooseInitialServerListener implements DynamicListener<PlayerChooseInitialServerEvent> {
	private final ProxyServer proxyServer;
	private final ConnectionStateRegistry connectionStateRegistry;
	private final EventManager eventManager;

	@Override
	public void onEvent(PlayerChooseInitialServerEvent event) {
		UUID connectionId = event.getPlayer().getUniqueId();
		RoutingTarget target = connectionStateRegistry.peekRoutingTarget(connectionId).orElse(null);
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
			connectionStateRegistry.find(connectionId)
					.ifPresent(ConnectionState::consumeRoutingTarget);
		event.setInitialServer(server.get());
	}
}
