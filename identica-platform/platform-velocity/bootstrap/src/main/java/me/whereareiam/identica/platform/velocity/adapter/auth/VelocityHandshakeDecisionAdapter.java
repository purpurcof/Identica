package me.whereareiam.identica.platform.velocity.adapter.auth;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.velocitypowered.api.event.AwaitingEventExecutor;
import com.velocitypowered.api.event.EventTask;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import me.whereareiam.identica.ConnectionCoordinator;
import me.whereareiam.identica.handshake.HandshakeDecisionAdapter;
import me.whereareiam.identica.handshake.HandshakeApplierRegistry;
import me.whereareiam.identica.handshake.HandshakeStore;
import me.whereareiam.identica.identity.actor.ConnectionIdentity;
import me.whereareiam.identica.logging.Logger;
import me.whereareiam.identica.model.auth.handshake.HandshakeDecision;
import me.whereareiam.identica.model.auth.handshake.HandshakeRequest;
import me.whereareiam.identica.model.config.Messages;
import me.whereareiam.identica.platform.velocity.api.handshake.VelocityHandshakeContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

@Singleton
public class VelocityHandshakeDecisionAdapter extends HandshakeDecisionAdapter implements AwaitingEventExecutor<PreLoginEvent> {
	private final @NotNull ConnectionCoordinator connectionCoordinator;
	private final @NotNull HandshakeStore handshakeStore;
	private final @NotNull HandshakeApplierRegistry<VelocityHandshakeContext> applierRegistry;

	@Inject
	public VelocityHandshakeDecisionAdapter(
			@NotNull ConnectionCoordinator connectionCoordinator,
			@NotNull HandshakeStore handshakeStore,
			@NotNull HandshakeApplierRegistry<VelocityHandshakeContext> applierRegistry,
			@NotNull Provider<Messages> messagesProvider
	) {
		super(messagesProvider);
		this.connectionCoordinator = connectionCoordinator;
		this.handshakeStore = handshakeStore;
		this.applierRegistry = applierRegistry;
	}

	@Override
	public EventTask executeAsync(PreLoginEvent event) {
		if (!event.getResult().isAllowed())
			return null;

		String resolvedIp = resolveIp(event);
		if (resolvedIp == null) {
			Logger.warn("PreLogin missing remote IP for %s, skipping handshake processing", event.getUsername());
			return null;
		}

		CompletableFuture<?> future = connectionCoordinator
				.handshake(new HandshakeRequest(new ConnectionIdentity(event.getUsername(), resolvedIp)))
				.whenComplete((decision, error) -> {
					if (error != null) {
						Logger.severe("Handshake failed", error);
						return;
					}

					HandshakeDecision resolved = decision != null ? decision : HandshakeDecision.allow();
					apply(resolved, target(event));

					if (resolved.getStatus() != HandshakeDecision.Status.DENY) {
						handshakeStore.consumeInstruction(event.getUsername(), resolvedIp)
								.ifPresent(instruction -> applierRegistry.applyAll(
										new VelocityHandshakeContext(event),
										instruction
								));
					}
				})
				.toCompletableFuture();

		return EventTask.resumeWhenComplete(future);
	}

	private @NotNull HandshakeDecisionTarget target(@NotNull PreLoginEvent event) {
		return message -> event.setResult(PreLoginEvent.PreLoginComponentResult.denied(message));
	}

	private @Nullable String resolveIp(@NotNull PreLoginEvent event) {
		if (event.getConnection().getRemoteAddress() == null) return null;

		if (event.getConnection().getRemoteAddress().getAddress() != null) {
			String hostAddress = event.getConnection().getRemoteAddress().getAddress().getHostAddress();
			if (hostAddress != null && !hostAddress.isBlank())
				return hostAddress;
		}

		String hostString = event.getConnection().getRemoteAddress().getHostString();
		if (hostString == null || hostString.isBlank()) return null;

		return hostString;
	}
}
