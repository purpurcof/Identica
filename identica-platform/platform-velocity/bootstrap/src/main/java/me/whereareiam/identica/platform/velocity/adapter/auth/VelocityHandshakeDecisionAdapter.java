package me.whereareiam.identica.platform.velocity.adapter.auth;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.velocitypowered.api.event.AwaitingEventExecutor;
import com.velocitypowered.api.event.EventTask;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import me.whereareiam.identica.ConnectionCoordinator;
import me.whereareiam.identica.connection.prepare.PrepareStateStore;
import me.whereareiam.identica.handshake.HandshakeDecisionAdapter;
import me.whereareiam.identica.handshake.HandshakeApplierRegistry;
import me.whereareiam.identica.handshake.HandshakeStore;
import me.whereareiam.identica.identity.actor.ConnectionIdentity;
import me.whereareiam.identica.logging.Logger;
import me.whereareiam.identica.model.config.Messages;
import me.whereareiam.identica.platform.velocity.api.handshake.VelocityHandshakeContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;

@Singleton
public class VelocityHandshakeDecisionAdapter extends HandshakeDecisionAdapter implements AwaitingEventExecutor<PreLoginEvent> {
	private final @NotNull HandshakeApplierRegistry<VelocityHandshakeContext> applierRegistry;

	@Inject
	public VelocityHandshakeDecisionAdapter(
			@NotNull ConnectionCoordinator connectionCoordinator,
			@NotNull PrepareStateStore prepareStateStore,
			@NotNull HandshakeStore handshakeStore,
			@NotNull HandshakeApplierRegistry<VelocityHandshakeContext> applierRegistry,
			@NotNull Provider<Messages> messagesProvider
	) {
		super(connectionCoordinator, prepareStateStore, handshakeStore, messagesProvider);
		this.applierRegistry = applierRegistry;
	}

	@Override
	public EventTask executeAsync(PreLoginEvent event) {
		if (!event.getResult().isAllowed()) return null;

		HandshakeAdapterRequest request = request(event);
		if (request == null) return null;

		CompletableFuture<?> future = CompletableFuture.completedFuture(null).thenCompose(ignored -> {
			if (!event.getResult().isAllowed())
				return CompletableFuture.completedFuture(null);

			return adapt(request, target(event)).toCompletableFuture();
		});

		return EventTask.resumeWhenComplete(future);
	}

	private @Nullable HandshakeAdapterRequest request(@NotNull PreLoginEvent event) {
		String resolvedIp = resolveIp(event);
		if (resolvedIp == null) {
			Logger.warn("PreLogin missing remote IP for %s, skipping handshake processing", event.getUsername());
			return null;
		}

		ConnectionIdentity identity = new ConnectionIdentity(event.getUsername(), resolvedIp);
		applyOrigin(identity, event);
		VelocityHandshakeContext context = new VelocityHandshakeContext(event);
		return new HandshakeAdapterRequest(
				identity,
				instruction -> applierRegistry.applyAll(context, instruction)
		);
	}

	private @NotNull HandshakeDecisionTarget target(@NotNull PreLoginEvent event) {
		return message -> event.setResult(PreLoginEvent.PreLoginComponentResult.denied(message));
	}

	private void applyOrigin(@NotNull ConnectionIdentity identity, @NotNull PreLoginEvent event) {
		InetSocketAddress virtualHost = event.getConnection().getVirtualHost().orElse(null);
		if (virtualHost == null) return;

		identity.setOrigin(new ConnectionIdentity.Origin(
				virtualHost.getHostString(),
				virtualHost.getPort()
		));
	}

	private @Nullable String resolveIp(@NotNull PreLoginEvent event) {
		if (event.getConnection().getRemoteAddress() == null) return null;

		if (event.getConnection().getRemoteAddress().getAddress() != null) {
			String hostAddress = event.getConnection().getRemoteAddress().getAddress().getHostAddress();
			if (hostAddress != null && !hostAddress.isBlank()) return hostAddress;
		}

		String hostString = event.getConnection().getRemoteAddress().getHostString();
		if (hostString == null || hostString.isBlank()) return null;

		return hostString;
	}
}
