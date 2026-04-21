package me.whereareiam.identica.platform.velocity.adapter.routing;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.velocitypowered.api.proxy.Player;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.routing.RoutingCoordinator;
import org.jetbrains.annotations.NotNull;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class VelocityRoutingIntentReachedEmitter {
	private final RoutingCoordinator routingCoordinator;

	public void emitIfReached(@NotNull Player player) {
		String currentServer = player.getCurrentServer()
				.map(server -> server.getServerInfo().getName())
				.orElse(null);
		if (currentServer == null) return;
		routingCoordinator.markReached(player.getUniqueId(), currentServer);
	}
}
