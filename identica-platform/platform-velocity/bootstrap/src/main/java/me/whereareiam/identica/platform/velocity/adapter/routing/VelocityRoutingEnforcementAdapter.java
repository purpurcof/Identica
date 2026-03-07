package me.whereareiam.identica.platform.velocity.adapter.routing;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.adapter.RoutingEnforcementAdapter;
import me.whereareiam.identica.event.EventListener;
import me.whereareiam.identica.event.base.IdenticEvent;
import me.whereareiam.identica.event.routing.RoutingTargetUpdatedEvent;
import me.whereareiam.identica.logging.Logger;
import me.whereareiam.identica.routing.RoutingStateStore;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class VelocityRoutingEnforcementAdapter extends RoutingEnforcementAdapter implements EventListener {
	private final ProxyServer proxyServer;
	private final RoutingStateStore routingStateStore;

	@IdenticEvent
	public void onRoutingTargetUpdated(@NotNull RoutingTargetUpdatedEvent event) {
		Player player = proxyServer.getPlayer(event.getConnectionUniqueId()).orElse(null);
		if (player == null) return;

		String currentServer = player.getCurrentServer()
				.map(server -> server.getServerInfo().getName())
				.orElse(null);

		applyRoutingUpdate(
				new RoutingUpdateContext(
						player.getUsername(),
						currentServer,
						event.getTarget()
				),
				new RoutingEnforcementTarget() {
					@Override
					public boolean connect(@NotNull String serverName) {
						Optional<RegisteredServer> server = proxyServer.getServer(serverName);
						if (server.isEmpty()) {
							Logger.debug("Routing target missing on proxy username=%s target=%s",
									player.getUsername(), serverName);
							return false;
						}

						player.createConnectionRequest(server.get()).fireAndForget();
						return true;
					}

					@Override
					public void consume() {
						routingStateStore.consume(event.getConnectionUniqueId());
					}
				});
	}
}
