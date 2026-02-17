package me.whereareiam.identica.platform.velocity.adapter.auth;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.proxy.Player;
import me.whereareiam.identica.ConnectionCoordinator;
import me.whereareiam.identica.adapter.ConnectionDecisionAdapter;
import me.whereareiam.identica.identity.actor.ConnectionIdentity;
import me.whereareiam.identica.listener.DynamicListener;
import me.whereareiam.identica.model.auth.ConnectionDecision;
import me.whereareiam.identica.model.auth.request.ConnectionRequest;
import me.whereareiam.identica.model.config.Messages;
import me.whereareiam.identica.platform.velocity.actor.VelocityCommandPlayer;
import me.whereareiam.identica.identity.IdentityService;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

@Singleton
public class VelocityLoginDecisionAdapter extends ConnectionDecisionAdapter implements DynamicListener<LoginEvent> {
	private final @NotNull ConnectionCoordinator connectionCoordinator;
	private final @NotNull IdentityService identityService;

	@Inject
	public VelocityLoginDecisionAdapter(
			@NotNull ConnectionCoordinator connectionCoordinator,
			@NotNull Provider<Messages> messagesProvider,
			@NotNull IdentityService identityService
	) {
		super(messagesProvider);
		this.connectionCoordinator = connectionCoordinator;
		this.identityService = identityService;
	}

	@Override
	public void onEvent(LoginEvent event) {
		Player player = event.getPlayer();
		String ip = resolveIp(player);
		String intendedServer = player.getCurrentServer()
				.map(server -> server.getServerInfo().getName())
				.orElse(null);

		ConnectionRequest request = ConnectionRequest.builder()
				.identity(new ConnectionIdentity(player.getUsername(), ip))
				.connectionUniqueId(player.getUniqueId())
				.intendedServer(intendedServer)
				.build();

		ConnectionDecision decision = connectionCoordinator.process(request)
				.toCompletableFuture()
				.join();

		apply(decision, new VelocityCommandPlayer(player), loginTarget(event));
		ConnectionDecision.Status status = decision != null ? decision.getStatus() : null;
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

	private @NotNull ConnectionDecisionTarget loginTarget(@NotNull LoginEvent event) {
		return new ConnectionDecisionTarget() {
			@Override
			public void deny(@NotNull Component message) {
				event.setResult(ResultedEvent.ComponentResult.denied(message));
			}

			@Override
			public void requireReconnect(@NotNull Component message) {
				event.setResult(ResultedEvent.ComponentResult.denied(message));
			}
		};
	}
}
