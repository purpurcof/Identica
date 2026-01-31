package me.whereareiam.identica.platform.velocity.listener.connection;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.Serializer;
import me.whereareiam.identica.auth.AuthenticationCoordinator;
import me.whereareiam.identica.identity.actor.ConnectionIdentity;
import me.whereareiam.identica.identity.registry.IdentityRegistry;
import me.whereareiam.identica.listener.DynamicListener;
import me.whereareiam.identica.model.auth.AuthDecision;
import me.whereareiam.identica.model.auth.request.ResumeRequest;
import me.whereareiam.identica.model.config.Messages;
import me.whereareiam.identica.platform.velocity.actor.VelocityCommandPlayer;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class PostConnectAuthListener implements DynamicListener<ServerConnectedEvent> {
	private final AuthenticationCoordinator authenticationCoordinator;
	private final Provider<Messages> messagesProvider;
	private final IdentityRegistry identityRegistry;

	@Override
	public void onEvent(ServerConnectedEvent event) {
		if (event == null || event.getPlayer() == null)
			return;

		if (event.getPreviousServer().isPresent())
			return;

		Player player = event.getPlayer();
		String ip = player.getRemoteAddress().getHostString();
		String intendedServer = player.getCurrentServer()
				.map(server -> server.getServerInfo().getName())
				.orElse(null);

		ResumeRequest request = ResumeRequest.builder()
				.connectionUniqueId(player.getUniqueId())
				.identity(new ConnectionIdentity(player.getUniqueId(), player.getUsername(), ip))
				.intendedServer(intendedServer)
				.build();

		AuthDecision decision = authenticationCoordinator.resume(request, null)
				.toCompletableFuture()
				.join();

		if (decision == null || decision.getStatus() == null)
			return;

		if (decision.getStatus() == AuthDecision.Status.NO_PENDING)
			return;

		VelocityCommandPlayer actor = new VelocityCommandPlayer(player);
		switch (decision.getStatus()) {
			case WAIT -> {
				if (decision.getMessage() != null && !decision.getMessage().isBlank())
					player.sendMessage(Serializer.serialize(actor, decision.getMessage()));
			}
			case DENY, REQUIRE_RECONNECT -> {
				identityRegistry.removePlayer(player.getUniqueId());
				player.disconnect(Serializer.serialize(actor, resolveAuthMessage(decision.getMessage())));
			}
			default -> {
			}
		}
	}

	private String resolveAuthMessage(String message) {
		if (message != null && !message.isBlank())
			return message;

		return String.join("\n", messagesProvider.get().getAuthentication().getAuthenticationFailed());
	}
}
