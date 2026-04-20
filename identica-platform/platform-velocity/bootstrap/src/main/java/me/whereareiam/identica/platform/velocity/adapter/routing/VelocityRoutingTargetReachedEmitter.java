package me.whereareiam.identica.platform.velocity.adapter.routing;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.velocitypowered.api.proxy.Player;
import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.event.routing.RoutingTargetReachedEvent;
import me.whereareiam.identica.model.RoutingTarget;
import me.whereareiam.identica.routing.RoutingStateStore;
import me.whereareiam.identica.type.RoutingTargetType;
import org.jetbrains.annotations.NotNull;

@Singleton
public class VelocityRoutingTargetReachedEmitter {
	private final EventManager eventManager;
	private final RoutingStateStore routingStateStore;

	@Inject
	public VelocityRoutingTargetReachedEmitter(
			@NotNull EventManager eventManager,
			@NotNull RoutingStateStore routingStateStore
	) {
		this.eventManager = eventManager;
		this.routingStateStore = routingStateStore;
	}

	public void emitIfReached(@NotNull Player player) {
		RoutingTarget target = routingStateStore.peek(player.getUniqueId()).orElse(null);
		if (target == null) return;

		String targetServer = target.getServer();
		if (targetServer == null || targetServer.isBlank())
			return;

		String currentServer = player.getCurrentServer()
				.map(server -> server.getServerInfo().getName())
				.orElse(null);
		if (currentServer == null || !currentServer.equalsIgnoreCase(targetServer))
			return;

		eventManager.call(new RoutingTargetReachedEvent(player.getUniqueId(), target, currentServer));
		if (target.getType() == RoutingTargetType.COMPLETED)
			routingStateStore.consume(player.getUniqueId());
	}
}
