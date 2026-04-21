package me.whereareiam.identica.common.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.whereareiam.identica.Serializer;
import me.whereareiam.identica.event.EventListener;
import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.event.base.IdenticEvent;
import me.whereareiam.identica.event.identity.session.SessionClosedEvent;
import me.whereareiam.identica.identity.IdentityService;
import me.whereareiam.identica.model.SessionCloseRequest;
import org.jetbrains.annotations.NotNull;

@Singleton
public class SessionClosedDisconnectListener implements EventListener {
	private final @NotNull IdentityService identityService;

	@Inject
	public SessionClosedDisconnectListener(
			@NotNull IdentityService identityService,
			@NotNull EventManager eventManager
	) {
		this.identityService = identityService;
		eventManager.register(this);
	}

	@IdenticEvent
	public void onSessionClosed(@NotNull SessionClosedEvent event) {
		SessionCloseRequest request = event.getRequest();
		if (request == null) return;

		String disconnectMessage = request.getDisconnectMessage();
		if (disconnectMessage == null || disconnectMessage.isBlank()) return;

		identityService.find(event.getUniqueId())
				.ifPresent(identity -> identity.disconnect(Serializer.serialize(identity, disconnectMessage)));
	}
}
