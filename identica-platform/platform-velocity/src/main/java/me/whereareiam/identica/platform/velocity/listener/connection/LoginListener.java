package me.whereareiam.identica.platform.velocity.listener.connection;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.util.GameProfile;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.Serializer;
import me.whereareiam.identica.auth.AuthCoordinator;
import me.whereareiam.identica.listener.DynamicListener;
import me.whereareiam.identica.model.auth.AuthDecision;
import me.whereareiam.identica.model.auth.LoginRequest;
import me.whereareiam.identica.model.config.Messages;
import me.whereareiam.identica.platform.velocity.actor.VelocityCommandPlayer;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class LoginListener implements DynamicListener<LoginEvent> {
	private final AuthCoordinator authCoordinator;
	private final Provider<Messages> messagesProvider;

	@Override
	public void onEvent(LoginEvent event) {
		Player player = event.getPlayer();
		String ip = player.getRemoteAddress().getHostString();
		String intendedServer = player.getCurrentServer()
				.map(server -> server.getServerInfo().getName())
				.orElse(null);

		LoginRequest request = LoginRequest.builder()
				.username(player.getUsername())
				.ip(ip)
				.connectionUniqueId(player.getUniqueId())
				.intendedServer(intendedServer)
				.onlineMode(player.isOnlineMode())
				.profileUniqueId(readProfileId(player))
				.build();

		AuthDecision decision = authCoordinator.authenticate(request);

		if (decision == null || decision.getStatus() == null)
			return;

		VelocityCommandPlayer actor = new VelocityCommandPlayer(player);
		switch (decision.getStatus()) {
			case WAIT -> {
				if (decision.getMessage() != null && !decision.getMessage().isBlank()) {
					player.sendMessage(Serializer.serialize(actor, decision.getMessage()));
				}
			}
			case DENY, REQUIRE_RECONNECT -> event.setResult(ResultedEvent.ComponentResult.denied(
					Serializer.serialize(actor, resolveAuthMessage(decision.getMessage()))
			));
			default -> {
			}
		}
	}

	private String resolveAuthMessage(String message) {
		if (message != null && !message.isBlank())
			return message;

		return joinMessage(messagesProvider.get().getAuthentication().getAuthenticationFailed());
	}

	private String joinMessage(java.util.List<String> lines) {
		if (lines == null || lines.isEmpty()) return "";
		return String.join("\n", lines);
	}

	private String readProfileId(Player player) {
		if (!player.isOnlineMode()) return null;

		GameProfile profile = player.getGameProfile();
		if (profile == null || profile.getId() == null)
			return null;

		return profile.getId().toString();
	}
}
