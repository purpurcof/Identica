package me.whereareiam.identica.platform.velocity.listener.connection.server;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.event.routing.RoutingTargetMissingEvent;
import me.whereareiam.identica.listener.DynamicListener;
import me.whereareiam.identica.model.routing.attempt.RoutingAttemptDecision;
import me.whereareiam.identica.model.routing.attempt.RoutingAttemptReport;
import me.whereareiam.identica.model.routing.attempt.RoutingAttemptRequest;
import me.whereareiam.identica.model.routing.RoutingIntent;
import me.whereareiam.identica.routing.RoutingAttemptService;
import me.whereareiam.identica.type.routing.RoutingAttemptTrigger;

import java.util.Optional;
import java.util.UUID;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ServerPreConnectListener implements DynamicListener<ServerPreConnectEvent> {
	private final ProxyServer proxyServer;
	private final RoutingAttemptService routingAttemptService;
	private final EventManager eventManager;

	@Override
	public void onEvent(ServerPreConnectEvent event) {
		UUID connectionId = event.getPlayer().getUniqueId();
		RoutingIntent currentIntent = routingAttemptService.current(connectionId).orElse(null);
		if (currentIntent == null) return;
		String targetServer = currentIntent.getEndpoint().getServer();
		if (targetServer.isBlank()) return;
		if (event.getOriginalServer().getServerInfo().getName().equalsIgnoreCase(targetServer)) {
			return;
		}

		String currentServer = event.getPlayer().getCurrentServer()
				.map(server -> server.getServerInfo().getName())
				.orElse(null);
		RoutingAttemptDecision decision = routingAttemptService.decide(new RoutingAttemptRequest(
				connectionId,
				RoutingAttemptTrigger.PRE_CONNECT,
				currentServer
		));
		if (!decision.isAllowed() || decision.getIntent() == null) return;

		RoutingIntent intent = decision.getIntent();
		targetServer = intent.getEndpoint().getServer();
		if (targetServer.isBlank()) return;

		Optional<RegisteredServer> server = proxyServer.getServer(targetServer);
		if (server.isEmpty()) {
			RoutingTargetMissingEvent missingEvent = new RoutingTargetMissingEvent(
					connectionId,
					event.getPlayer().getUsername(),
					intent
			);
			eventManager.call(missingEvent);
			if (missingEvent.isDisconnect() && missingEvent.getMessage() != null)
				event.getPlayer().disconnect(missingEvent.getMessage());

			routingAttemptService.record(new RoutingAttemptReport(
					connectionId,
					RoutingAttemptTrigger.PRE_CONNECT,
					false,
					targetServer,
					"missing-server"
			));
			return;
		}

		event.setResult(ServerPreConnectEvent.ServerResult.allowed(server.get()));
		routingAttemptService.record(new RoutingAttemptReport(
				connectionId,
				RoutingAttemptTrigger.PRE_CONNECT,
				true,
				targetServer,
				null
		));
	}
}
