package me.whereareiam.identica.platform.velocity.listener.connection;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.proxy.Player;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.Serializer;
import me.whereareiam.identica.identity.actor.OfflineIdentity;
import me.whereareiam.identica.auth.AuthenticationCoordinator;
import me.whereareiam.identica.listener.DynamicListener;
import me.whereareiam.identica.model.auth.AuthDecision;
import me.whereareiam.identica.model.auth.ConnectionInfo;
import me.whereareiam.identica.model.auth.request.LoginRequest;
import me.whereareiam.identica.model.config.Messages;
import me.whereareiam.identica.platform.velocity.actor.VelocityCommandPlayer;
import me.whereareiam.identica.registry.IdentityRegistry;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class LoginListener implements DynamicListener<LoginEvent> {
	private final AuthenticationCoordinator authenticationCoordinator;
	private final Provider<Messages> messagesProvider;
	private final IdentityRegistry identityRegistry;

	@Override
	public void onEvent(LoginEvent event) {
		Player player = event.getPlayer();
		String ip = player.getRemoteAddress().getHostString();
		String intendedServer = player.getCurrentServer()
				.map(server -> server.getServerInfo().getName())
				.orElse(null);

		LoginRequest request = LoginRequest.builder()
				.connectionInfo(ConnectionInfo.builder()
						.identity(new OfflineIdentity(player.getUniqueId(), player.getUsername(), ip))
						.onlineMode(player.isOnlineMode())
						.build())
				.connectionUniqueId(player.getUniqueId())
				.intendedServer(intendedServer)
				.build();

		AuthDecision decision = authenticationCoordinator.authenticate(request)
				.toCompletableFuture()
				.join();

		if (decision.getStatus() == null)
			return;

		VelocityCommandPlayer actor = new VelocityCommandPlayer(player);
		identityRegistry.addPlayer(actor);
		switch (decision.getStatus()) {
			case WAIT -> {
				if (decision.getMessage() != null && !decision.getMessage().isBlank()) {
					player.sendMessage(Serializer.serialize(actor, decision.getMessage()));
				}
			}
			case DENY, REQUIRE_RECONNECT -> {
				identityRegistry.removePlayer(player.getUniqueId());
				event.setResult(ResultedEvent.ComponentResult.denied(
						Serializer.serialize(actor, resolveAuthMessage(decision.getMessage()))
				));
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
