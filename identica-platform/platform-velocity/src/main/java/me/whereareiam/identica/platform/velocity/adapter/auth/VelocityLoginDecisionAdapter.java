package me.whereareiam.identica.platform.velocity.adapter.auth;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.proxy.Player;
import me.whereareiam.identica.auth.AuthenticationCoordinator;
import me.whereareiam.identica.auth.adapter.AuthenticationDecisionAdapter;
import me.whereareiam.identica.identity.actor.ConnectionIdentity;
import me.whereareiam.identica.listener.DynamicListener;
import me.whereareiam.identica.model.auth.AuthDecision;
import me.whereareiam.identica.model.auth.request.LoginRequest;
import me.whereareiam.identica.model.config.Messages;
import me.whereareiam.identica.platform.velocity.actor.VelocityCommandPlayer;
import me.whereareiam.identica.identity.IdentityService;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

@Singleton
public class VelocityLoginDecisionAdapter extends AuthenticationDecisionAdapter implements DynamicListener<LoginEvent> {
	private final @NotNull AuthenticationCoordinator authenticationCoordinator;
	private final @NotNull IdentityService identityService;

	@Inject
	public VelocityLoginDecisionAdapter(
			@NotNull AuthenticationCoordinator authenticationCoordinator,
			@NotNull Provider<Messages> messagesProvider,
			@NotNull IdentityService identityService
	) {
		super(messagesProvider);
		this.authenticationCoordinator = authenticationCoordinator;
		this.identityService = identityService;
	}

	@Override
	public void onEvent(LoginEvent event) {
		Player player = event.getPlayer();
		String ip = player.getRemoteAddress().getHostString();
		String intendedServer = player.getCurrentServer()
				.map(server -> server.getServerInfo().getName())
				.orElse(null);

		LoginRequest request = LoginRequest.builder()
				.identity(new ConnectionIdentity(player.getUniqueId(), player.getUsername(), ip))
				.connectionUniqueId(player.getUniqueId())
				.intendedServer(intendedServer)
				.build();

		AuthDecision decision = authenticationCoordinator.authenticate(request)
				.toCompletableFuture()
				.join();

		apply(decision, new VelocityCommandPlayer(player), loginTarget(event));
		attachPresenceIfAllowed(decision, player);
	}

	private void attachPresenceIfAllowed(@NotNull AuthDecision decision, @NotNull Player player) {
		if (decision.getStatus() == null) return;
		switch (decision.getStatus()) {
			case ALLOW, WAIT -> identityService.attach(new VelocityCommandPlayer(player));
			default -> {
			}
		}
	}

	private @NotNull AuthenticationDecisionTarget loginTarget(@NotNull LoginEvent event) {
		return new AuthenticationDecisionTarget() {
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
