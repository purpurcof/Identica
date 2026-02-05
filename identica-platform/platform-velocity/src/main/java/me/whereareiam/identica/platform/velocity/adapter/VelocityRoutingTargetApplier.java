package me.whereareiam.identica.platform.velocity.adapter;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.velocitypowered.api.proxy.ProxyServer;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.Serializer;
import me.whereareiam.identica.logging.Logger;
import me.whereareiam.identica.model.RoutingTarget;
import me.whereareiam.identica.model.auth.AuthContext;
import me.whereareiam.identica.model.config.Messages;
import me.whereareiam.identica.routing.RoutingTargetApplier;
import me.whereareiam.identica.routing.RoutingStateStore;
import me.whereareiam.identica.type.RoutingTargetType;

import java.util.UUID;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class VelocityRoutingTargetApplier implements RoutingTargetApplier {
	private final ProxyServer proxyServer;
	private final RoutingStateStore routingStateStore;
	private final Provider<Messages> messagesProvider;

	@Override
	public void apply(RoutingTarget target, AuthContext context) {
		UUID connectionId = context.getConnectionUniqueId();
		if (connectionId == null) return;

		proxyServer.getPlayer(connectionId).ifPresent(player -> {
			if (player.getCurrentServer().isEmpty()) return;

			String current = player.getCurrentServer()
					.map(server -> server.getServerInfo().getName())
					.orElse(null);
			if (current != null && current.equalsIgnoreCase(target.getServer())) {
				if (target.getType() == RoutingTargetType.COMPLETED)
					routingStateStore.consume(connectionId);

				return;
			}

			proxyServer.getServer(target.getServer()).ifPresentOrElse(server -> {
				player.createConnectionRequest(server).fireAndForget();
				if (target.getType() == RoutingTargetType.COMPLETED) {
					routingStateStore.consume(connectionId);
				}
			}, () -> {
				Logger.warn("Routing target server %s not found for %s", target.getServer(), player.getUsername());

				String message = String.join("\n", messagesProvider.get().getAuthentication().getRouting().getMissingServer());
				if (!message.isBlank()) player.disconnect(Serializer.serialize(message));
				routingStateStore.clear(connectionId);
			});
		});
	}
}
