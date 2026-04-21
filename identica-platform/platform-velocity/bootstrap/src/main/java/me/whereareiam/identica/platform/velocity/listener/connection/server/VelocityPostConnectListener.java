package me.whereareiam.identica.platform.velocity.listener.connection.server;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.listener.DynamicListener;
import me.whereareiam.identica.platform.velocity.adapter.auth.VelocityResumeDecisionAdapter;
import me.whereareiam.identica.routing.RoutingCoordinator;
import org.jetbrains.annotations.NotNull;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class VelocityPostConnectListener implements DynamicListener<ServerPostConnectEvent> {
	private final VelocityResumeDecisionAdapter resumeDecisionAdapter;
	private final RoutingCoordinator routingCoordinator;

	@Override
	public void onEvent(ServerPostConnectEvent event) {
		resumeDecisionAdapter.resume(event);
		markRoutingReached(event);
	}

	private void markRoutingReached(@NotNull ServerPostConnectEvent event) {
		String currentServer = event.getPlayer().getCurrentServer()
				.map(server -> server.getServerInfo().getName())
				.orElse(null);
		if (currentServer == null) return;

		routingCoordinator.markReached(event.getPlayer().getUniqueId(), currentServer);
	}
}
