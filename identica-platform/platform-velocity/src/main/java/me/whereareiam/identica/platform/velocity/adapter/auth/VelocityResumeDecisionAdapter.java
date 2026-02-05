package me.whereareiam.identica.platform.velocity.adapter.auth;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;
import me.whereareiam.identica.auth.AuthenticationCoordinator;
import me.whereareiam.identica.auth.adapter.AuthenticationDecisionAdapter;
import me.whereareiam.identica.identity.actor.ConnectionIdentity;
import me.whereareiam.identica.listener.DynamicListener;
import me.whereareiam.identica.model.auth.AuthDecision;
import me.whereareiam.identica.model.auth.request.ResumeRequest;
import me.whereareiam.identica.model.config.Messages;
import me.whereareiam.identica.platform.velocity.actor.VelocityCommandPlayer;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

@Singleton
public class VelocityResumeDecisionAdapter extends AuthenticationDecisionAdapter implements DynamicListener<ServerConnectedEvent> {
	private final @NotNull AuthenticationCoordinator authenticationCoordinator;

	@Inject
	public VelocityResumeDecisionAdapter(
			@NotNull AuthenticationCoordinator authenticationCoordinator,
			@NotNull Provider<Messages> messagesProvider
	) {
		super(messagesProvider);
		this.authenticationCoordinator = authenticationCoordinator;
	}

	@Override
	public void onEvent(ServerConnectedEvent event) {
		if (event.getPreviousServer().isPresent()) return;

		Player player = event.getPlayer();
		String ip = resolveIp(player);
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

		if (decision != null && decision.getStatus() == AuthDecision.Status.NO_PENDING)
			return;

		apply(decision, new VelocityCommandPlayer(player), resumeTarget(player));
	}

	private String resolveIp(@NotNull Player player) {
		if (player.getRemoteAddress() == null)
			return null;

		if (player.getRemoteAddress().getAddress() != null)
			return player.getRemoteAddress().getAddress().getHostAddress();

		return player.getRemoteAddress().getHostString();
	}

	private @NotNull AuthenticationDecisionTarget resumeTarget(@NotNull Player player) {
		return new AuthenticationDecisionTarget() {
			@Override
			public void deny(@NotNull Component message) {
				player.disconnect(message);
			}

			@Override
			public void requireReconnect(@NotNull Component message) {
				player.disconnect(message);
			}
		};
	}
}
