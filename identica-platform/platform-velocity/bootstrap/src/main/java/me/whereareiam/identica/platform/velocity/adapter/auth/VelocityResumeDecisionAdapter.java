package me.whereareiam.identica.platform.velocity.adapter.auth;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;
import me.whereareiam.identica.ConnectionCoordinator;
import me.whereareiam.identica.adapter.ConnectionDecisionAdapter;
import me.whereareiam.identica.identity.actor.ConnectionIdentity;
import me.whereareiam.identica.listener.DynamicListener;
import me.whereareiam.identica.model.auth.ConnectionDecision;
import me.whereareiam.identica.model.auth.request.ResumeRequest;
import me.whereareiam.identica.model.config.Messages;
import me.whereareiam.identica.platform.velocity.actor.VelocityCommandPlayer;
import me.whereareiam.identica.identity.IdentityService;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

@Singleton
public class VelocityResumeDecisionAdapter extends ConnectionDecisionAdapter implements DynamicListener<ServerConnectedEvent> {
	private final @NotNull ConnectionCoordinator connectionCoordinator;
	private final @NotNull IdentityService identityService;

	@Inject
	public VelocityResumeDecisionAdapter(
			@NotNull ConnectionCoordinator connectionCoordinator,
			@NotNull Provider<Messages> messagesProvider,
			@NotNull IdentityService identityService
	) {
		super(messagesProvider);
		this.connectionCoordinator = connectionCoordinator;
		this.identityService = identityService;
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

		ConnectionDecision decision = connectionCoordinator.resume(request)
				.toCompletableFuture()
				.join();

		if (decision == null || decision.getStatus() == ConnectionDecision.Status.NO_PENDING)
			return;

		apply(decision, new VelocityCommandPlayer(player), resumeTarget(player));
		ConnectionDecision.Status status = decision.getStatus();
		if (status == ConnectionDecision.Status.ALLOW || status == ConnectionDecision.Status.WAIT) {
			identityService.attach(new VelocityCommandPlayer(player));
		}
	}

	private String resolveIp(@NotNull Player player) {
		if (player.getRemoteAddress() == null)
			return null;

		if (player.getRemoteAddress().getAddress() != null)
			return player.getRemoteAddress().getAddress().getHostAddress();

		return player.getRemoteAddress().getHostString();
	}

	private @NotNull ConnectionDecisionTarget resumeTarget(@NotNull Player player) {
		return new ConnectionDecisionTarget() {
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
