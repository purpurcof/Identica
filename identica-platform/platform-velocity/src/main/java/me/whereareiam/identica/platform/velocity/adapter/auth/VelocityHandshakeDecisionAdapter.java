package me.whereareiam.identica.platform.velocity.adapter.auth;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.velocitypowered.api.event.AwaitingEventExecutor;
import com.velocitypowered.api.event.EventTask;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import me.whereareiam.identica.auth.AuthenticationCoordinator;
import me.whereareiam.identica.auth.adapter.HandshakeDecisionAdapter;
import me.whereareiam.identica.identity.actor.ConnectionIdentity;
import me.whereareiam.identica.logging.Logger;
import me.whereareiam.identica.model.auth.handshake.HandshakeRequest;
import me.whereareiam.identica.model.config.Messages;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

@Singleton
public class VelocityHandshakeDecisionAdapter extends HandshakeDecisionAdapter implements AwaitingEventExecutor<PreLoginEvent> {
	private final @NotNull AuthenticationCoordinator authenticationCoordinator;

	@Inject
	public VelocityHandshakeDecisionAdapter(
			@NotNull AuthenticationCoordinator authenticationCoordinator,
			@NotNull Provider<Messages> messagesProvider
	) {
		super(messagesProvider);
		this.authenticationCoordinator = authenticationCoordinator;
	}

	@Override
	public EventTask executeAsync(PreLoginEvent event) {
		if (!event.getResult().isAllowed())
			return null;

		CompletableFuture<?> future = authenticationCoordinator
				.handshake(new HandshakeRequest(new ConnectionIdentity(event.getUsername(), null)))
				.whenComplete((decision, error) -> {
					if (error != null) {
						Logger.severe("Handshake failed", error);
						return;
					}

					if (decision != null)
						apply(decision, target(event));
				})
				.toCompletableFuture();

		return EventTask.resumeWhenComplete(future);
	}

	private @NotNull HandshakeDecisionTarget target(@NotNull PreLoginEvent event) {
		return new HandshakeDecisionTarget() {
			@Override
			public void forceOnline() {
				event.setResult(PreLoginEvent.PreLoginComponentResult.forceOnlineMode());
			}

			@Override
			public void forceOffline() {
				event.setResult(PreLoginEvent.PreLoginComponentResult.forceOfflineMode());
			}

			@Override
			public void deny(@NotNull Component message) {
				event.setResult(PreLoginEvent.PreLoginComponentResult.denied(message));
			}
		};
	}
}
