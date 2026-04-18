package me.whereareiam.identica.platform.velocity.adapter.auth;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.proxy.Player;
import me.whereareiam.identica.ConnectionCoordinator;
import me.whereareiam.identica.adapter.ConnectionDecisionAdapter;
import me.whereareiam.identica.pipeline.prepare.PrepareStateStore;
import me.whereareiam.identica.identity.IdentityService;
import me.whereareiam.identica.identity.actor.ConnectionIdentity;
import me.whereareiam.identica.listener.DynamicListener;
import me.whereareiam.identica.model.auth.ConnectionDecision;
import me.whereareiam.identica.model.auth.request.ResumeRequest;
import me.whereareiam.identica.model.config.Messages;
import me.whereareiam.identica.model.pipeline.prepare.decision.PrepareDecision;
import me.whereareiam.identica.model.provider.ProviderContext;
import me.whereareiam.identica.platform.velocity.actor.VelocityCommandPlayer;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.net.InetSocketAddress;

@Singleton
public class VelocityResumeDecisionAdapter extends ConnectionDecisionAdapter implements DynamicListener<ServerPostConnectEvent> {
	private final @NotNull ConnectionCoordinator connectionCoordinator;
	private final @NotNull IdentityService identityService;
	private final @NotNull PrepareStateStore prepareStateStore;

	@Inject
	public VelocityResumeDecisionAdapter(
			@NotNull ConnectionCoordinator connectionCoordinator,
			@NotNull Provider<Messages> messagesProvider,
			@NotNull IdentityService identityService,
			@NotNull PrepareStateStore prepareStateStore
	) {
		super(messagesProvider);
		this.connectionCoordinator = connectionCoordinator;
		this.identityService = identityService;
		this.prepareStateStore = prepareStateStore;
	}

	@Override
	public void onEvent(ServerPostConnectEvent event) {
		if (event.getPreviousServer() != null)
			return;

		Player player = event.getPlayer();
		String ip = resolveIp(player);
		String intendedServer = player.getCurrentServer()
				.map(server -> server.getServerInfo().getName())
				.orElse(null);
		PrepareDecision prepared = prepareStateStore.peek(player.getUniqueId()).orElse(null);

		ProviderContext provider = prepared != null ? prepared.getProvider() : null;
		String providerUsername = provider != null && !provider.getProviderUsername().isBlank()
				? provider.getProviderUsername()
				: player.getUsername();
		ConnectionIdentity identity = new ConnectionIdentity(player.getUniqueId(), providerUsername, ip);
		applyOrigin(identity, player);

		ResumeRequest request = ResumeRequest.builder()
				.connectionUniqueId(player.getUniqueId())
				.identity(identity)
				.intendedServer(intendedServer)
				.provider(provider)
				.build();
		ConnectionDecision decision = connectionCoordinator.resume(request)
				.toCompletableFuture()
				.join();

		VelocityCommandPlayer liveIdentity = new VelocityCommandPlayer(player, identity.getUsername());
		if (decision == null || decision.getStatus() == ConnectionDecision.Status.NO_PENDING)
			return;

		apply(decision, liveIdentity, resumeTarget(player));
		ConnectionDecision.Status status = decision.getStatus();
		if (status == ConnectionDecision.Status.DENY || status == ConnectionDecision.Status.REQUIRE_RECONNECT)
			return;

		if (status == ConnectionDecision.Status.ALLOW || status == ConnectionDecision.Status.WAIT)
			identityService.attach(liveIdentity);
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

	private void applyOrigin(@NotNull ConnectionIdentity identity, @NotNull Player player) {
		InetSocketAddress virtualHost = player.getVirtualHost().orElse(null);
		if (virtualHost == null) return;

		identity.setOrigin(new ConnectionIdentity.Origin(
				virtualHost.getHostString(),
				virtualHost.getPort()
		));
	}
}
