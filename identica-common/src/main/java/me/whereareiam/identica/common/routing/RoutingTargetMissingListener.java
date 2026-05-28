package me.whereareiam.identica.common.routing;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import me.whereareiam.identica.Serializer;
import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.event.EventListener;
import me.whereareiam.identica.event.base.IdenticEvent;
import me.whereareiam.identica.event.routing.RoutingTargetMissingEvent;
import me.whereareiam.identica.logging.Logger;
import me.whereareiam.identica.model.config.Messages;
import me.whereareiam.identica.model.routing.RoutingIntent;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Singleton
public class RoutingTargetMissingListener implements EventListener {
	private final Provider<Messages> messagesProvider;

	@Inject
	public RoutingTargetMissingListener(
			Provider<Messages> messagesProvider,
			EventManager eventManager
	) {
		this.messagesProvider = messagesProvider;
		eventManager.register(this);
	}

	@IdenticEvent
	public void onTargetMissing(RoutingTargetMissingEvent event) {
		RoutingIntent intent = event.getIntent();
		String server = intent.getEndpoint().getServer();
		if (server == null || server.isBlank()) return;

		String actor = event.getUsername() != null && !event.getUsername().isBlank()
				? event.getUsername()
				: event.getConnectionUniqueId().toString();
		Logger.warn("Routing target server %s not found for %s", server, actor);

		event.setDisconnect(true);
		if (event.getMessage() == null || event.getMessage().equals(Component.empty())) {
			event.setMessage(buildMissingServerMessage());
		}
	}

	private Component buildMissingServerMessage() {
		Messages.Routing routing = messagesProvider.get().getRouting();
		String message = joinMessage(routing.getMissingServer());
		if (message.isBlank())
			return Component.empty();

		return Serializer.serialize(message);
	}

	private String joinMessage(@NotNull List<String> lines) {
		return String.join("\n", lines);
	}
}
