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
public class PlayerChooseInitialServerListener implements DynamicListener<PlayerChooseInitialServerEvent> {
	private final ProxyServer proxyServer;
	private final RoutingAttemptService routingAttemptService;
	private final EventManager eventManager;

	@Override
	public void onEvent(PlayerChooseInitialServerEvent event) {
		UUID connectionId = event.getPlayer().getUniqueId();
		RoutingAttemptDecision decision = routingAttemptService.decide(new RoutingAttemptRequest(
				connectionId,
				RoutingAttemptTrigger.INITIAL_SERVER,
				null
		));
		if (!decision.isAllowed() || decision.getIntent() == null) return;

		RoutingIntent intent = decision.getIntent();
		String targetServer = intent.getEndpoint().getServer();
		if (targetServer.isBlank()) return;

		Optional<RegisteredServer> server = proxyServer.getServer(targetServer);
		if (server.isEmpty()) {
			RoutingTargetMissingEvent missingEvent = new RoutingTargetMissingEvent(
					connectionId,
					event.getPlayer().getUsername(),
					intent
			);
			eventManager.call(missingEvent);
			if (missingEvent.isDisconnect() && missingEvent.getMessage() != null) {
				event.getPlayer().disconnect(missingEvent.getMessage());
			}
			routingAttemptService.record(new RoutingAttemptReport(
					connectionId,
					RoutingAttemptTrigger.INITIAL_SERVER,
					false,
					targetServer,
					"missing-server"
			));
			return;
		}

		event.setInitialServer(server.get());
		routingAttemptService.record(new RoutingAttemptReport(
				connectionId,
				RoutingAttemptTrigger.INITIAL_SERVER,
				true,
				targetServer,
				null
		));
	}
}
