package me.whereareiam.identica.platform.velocity.listener.connection;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.listener.DynamicListener;
import me.whereareiam.identica.logging.Logger;
import me.whereareiam.identica.model.RoutingTarget;
import me.whereareiam.identica.type.RoutingTargetType;
import me.whereareiam.identica.routing.RoutingStateStore;

import java.util.Optional;
import java.util.UUID;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class RoutingPreConnectListener implements DynamicListener<ServerPreConnectEvent> {
	private final ProxyServer proxyServer;
	private final RoutingStateStore routingStateStore;

	@Override
	public void onEvent(ServerPreConnectEvent event) {
		UUID connectionId = event.getPlayer().getUniqueId();
		Optional<RoutingTarget> targetOptional = routingStateStore.peek(connectionId);
		if (targetOptional.isEmpty()) return;

		RoutingTarget target = targetOptional.get();
		if (target.getType() != RoutingTargetType.STEP) return;
		if (target.getServer() == null || target.getServer().isBlank()) return;

		Optional<RegisteredServer> server = proxyServer.getServer(target.getServer());
		if (server.isEmpty()) {
			Logger.warn("Routing target server %s not found for %s", target.getServer(), event.getPlayer().getUsername());
			return;
		}

		if (event.getOriginalServer().getServerInfo().getName().equalsIgnoreCase(target.getServer())) {
			return;
		}

		event.setResult(ServerPreConnectEvent.ServerResult.allowed(server.get()));
	}
}
