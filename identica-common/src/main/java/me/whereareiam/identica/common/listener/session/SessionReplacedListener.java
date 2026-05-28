package me.whereareiam.identica.common.listener.session;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import me.whereareiam.identica.Serializer;
import me.whereareiam.identica.event.EventListener;
import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.event.base.IdenticEvent;
import me.whereareiam.identica.event.identity.session.SessionReplacedEvent;
import me.whereareiam.identica.identity.IdentityService;
import me.whereareiam.identica.model.config.Messages;
import org.jetbrains.annotations.NotNull;

/**
 * Disconnects the superseded online identity when a session is replaced.
 */
@Singleton
public class SessionReplacedListener implements EventListener {
	private final @NotNull IdentityService identityService;
	private final @NotNull Provider<Messages> messagesProvider;

	@Inject
	public SessionReplacedListener(
			@NotNull IdentityService identityService,
			@NotNull Provider<Messages> messagesProvider,
			@NotNull EventManager eventManager
	) {
		this.identityService = identityService;
		this.messagesProvider = messagesProvider;
		eventManager.register(this);
	}

	@IdenticEvent
	public void onSessionReplaced(@NotNull SessionReplacedEvent event) {
		identityService.findByAccountUniqueId(event.getExistingSession().getUniqueId())
				.ifPresent(identity -> identity.disconnect(Serializer.serialize(identity, String.join("\n",
						messagesProvider.get()
								.getEngine()
								.getConcurrentLoginKick()
				))));
	}
}
