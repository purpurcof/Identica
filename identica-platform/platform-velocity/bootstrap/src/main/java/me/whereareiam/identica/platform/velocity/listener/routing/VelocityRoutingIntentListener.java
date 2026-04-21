package me.whereareiam.identica.platform.velocity.listener.routing;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import me.whereareiam.identica.event.EventListener;
import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.event.base.IdenticEvent;
import me.whereareiam.identica.event.routing.RoutingTargetMissingEvent;
import me.whereareiam.identica.event.routing.intent.RoutingIntentStartedEvent;
import me.whereareiam.identica.event.routing.intent.RoutingIntentUpdatedEvent;
import me.whereareiam.identica.logging.Logger;
import me.whereareiam.identica.model.routing.RoutingIntent;
import me.whereareiam.identica.model.routing.attempt.RoutingAttemptDecision;
import me.whereareiam.identica.model.routing.attempt.RoutingAttemptReport;
import me.whereareiam.identica.model.routing.attempt.RoutingAttemptRequest;
import me.whereareiam.identica.routing.RoutingAttemptService;
import me.whereareiam.identica.type.routing.RoutingAttemptTrigger;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

@Singleton
public class VelocityRoutingIntentListener implements EventListener {
	private final ProxyServer proxyServer;
	private final RoutingAttemptService routingAttemptService;
	private final EventManager eventManager;

	@Inject
	public VelocityRoutingIntentListener(
			@NotNull ProxyServer proxyServer,
			@NotNull RoutingAttemptService routingAttemptService,
			@NotNull EventManager eventManager
	) {
		this.proxyServer = proxyServer;
		this.routingAttemptService = routingAttemptService;
		this.eventManager = eventManager;
		eventManager.register(this);
	}

	@IdenticEvent
	public void onRoutingIntentStarted(@NotNull RoutingIntentStartedEvent event) {
		apply(event.getIntent(), "started");
	}

	@IdenticEvent
	public void onRoutingIntentUpdated(@NotNull RoutingIntentUpdatedEvent event) {
		apply(event.getIntent(), "updated");
	}

	private void apply(@NotNull RoutingIntent intent, @NotNull String source) {
		Player player = proxyServer.getPlayer(intent.getConnectionUniqueId()).orElse(null);
		if (player == null) {
			Logger.debug("Velocity routing intent %s skipped connection=%s target=%s reason=player-offline",
					source, intent.getConnectionUniqueId(), intent.getEndpoint().getServer());
			return;
		}

		String currentServer = player.getCurrentServer()
				.map(server -> server.getServerInfo().getName())
				.orElse(null);
		if (currentServer == null) {
			Logger.debug("Velocity routing intent %s skipped player=%s target=%s reason=no-current-server",
					source, player.getUniqueId(), intent.getEndpoint().getServer());
			return;
		}

		RoutingAttemptDecision decision = routingAttemptService.decide(new RoutingAttemptRequest(
				player.getUniqueId(),
				RoutingAttemptTrigger.ASYNC_CONNECT,
				currentServer
		));
		if (!decision.isAllowed() || decision.getIntent() == null) {
			Logger.debug("Velocity routing intent %s skipped player=%s current=%s target=%s reason=%s exhausted=%s",
					source,
					player.getUniqueId(),
					currentServer,
					intent.getEndpoint().getServer(),
					decision.getReason(),
					decision.isExhausted());
			return;
		}

		RoutingIntent currentIntent = decision.getIntent();
		String targetServer = currentIntent.getEndpoint().getServer();
		if (targetServer.isBlank()) {
			Logger.debug("Velocity routing intent %s skipped player=%s reason=blank-target",
					source, player.getUniqueId());
			return;
		}

		Optional<RegisteredServer> server = proxyServer.getServer(targetServer);
		if (server.isEmpty()) {
			Logger.debug("Velocity routing intent %s target missing player=%s target=%s",
					source, player.getUniqueId(), targetServer);
			RoutingTargetMissingEvent missingEvent = new RoutingTargetMissingEvent(
					player.getUniqueId(),
					player.getUsername(),
					currentIntent
			);
			eventManager.call(missingEvent);
			if (missingEvent.isDisconnect() && missingEvent.getMessage() != null)
				player.disconnect(missingEvent.getMessage());

			routingAttemptService.record(new RoutingAttemptReport(
					player.getUniqueId(),
					RoutingAttemptTrigger.ASYNC_CONNECT,
					false,
					targetServer,
					"missing-server"
			));
			return;
		}

		Logger.debug("Velocity routing intent %s applying player=%s current=%s target=%s",
				source, player.getUniqueId(), currentServer, targetServer);
		player.createConnectionRequest(server.get()).fireAndForget();
		routingAttemptService.record(new RoutingAttemptReport(
				player.getUniqueId(),
				RoutingAttemptTrigger.ASYNC_CONNECT,
				true,
				targetServer,
				null
		));
	}
}
