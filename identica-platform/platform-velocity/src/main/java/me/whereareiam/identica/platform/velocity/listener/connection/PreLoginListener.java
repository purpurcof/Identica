package me.whereareiam.identica.platform.velocity.listener.connection;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.velocitypowered.api.event.AwaitingEventExecutor;
import com.velocitypowered.api.event.EventTask;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.Serializer;
import me.whereareiam.identica.auth.AuthCoordinator;
import me.whereareiam.identica.logging.Logger;
import me.whereareiam.identica.model.auth.HandshakeDecision;
import me.whereareiam.identica.model.auth.HandshakeRequest;
import me.whereareiam.identica.model.config.Messages;

import java.util.concurrent.CompletableFuture;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class PreLoginListener implements AwaitingEventExecutor<PreLoginEvent> {
	private final AuthCoordinator authCoordinator;
	private final Provider<Messages> messagesProvider;

	@Override
	public EventTask executeAsync(PreLoginEvent event) {
		if (!event.getResult().isAllowed()) return null;

		CompletableFuture<?> future = authCoordinator
				.handshake(new HandshakeRequest(event.getUsername()))
				.whenComplete((decision, error) -> {
					if (error != null) {
						Logger.severe("Handshake failed", error);
						return;
					}

					if (decision != null) applyDecision(event, decision);
				})
				.toCompletableFuture();

		return EventTask.resumeWhenComplete(future);
	}

	private void applyDecision(PreLoginEvent event, HandshakeDecision decision) {
		switch (decision.getStatus()) {
			case FORCE_ONLINE -> event.setResult(PreLoginEvent.PreLoginComponentResult.forceOnlineMode());
			case FORCE_OFFLINE -> event.setResult(PreLoginEvent.PreLoginComponentResult.forceOfflineMode());
			case DENY -> event.setResult(PreLoginEvent.PreLoginComponentResult.denied(
					Serializer.serialize(resolveHandshakeMessage(decision.getMessage()))
			));
			default -> {
			}
		}
	}

	private String resolveHandshakeMessage(String message) {
		if (message != null && !message.isBlank())
			return message;

		return joinMessage(messagesProvider.get().getAuthentication().getHandshakeDenied());
	}

	private String joinMessage(java.util.List<String> lines) {
		if (lines == null || lines.isEmpty()) return "";
		return String.join("\n", lines);
	}
}
