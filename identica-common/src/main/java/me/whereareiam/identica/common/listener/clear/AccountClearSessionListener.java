package me.whereareiam.identica.common.listener.clear;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.whereareiam.identica.event.EventListener;
import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.event.account.AccountClearEvent;
import me.whereareiam.identica.event.base.IdenticEvent;
import me.whereareiam.identica.identity.IdentityService;
import me.whereareiam.identica.identity.actor.Identity;
import me.whereareiam.identica.identity.session.SessionService;
import me.whereareiam.identica.type.event.EventOrder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@Singleton
public class AccountClearSessionListener implements EventListener {
	private final SessionService sessionService;
	private final IdentityService identityService;

	@Inject
	public AccountClearSessionListener(
			@NotNull SessionService sessionService,
			@NotNull IdentityService identityService,
			@NotNull EventManager eventManager
	) {
		this.sessionService = sessionService;
		this.identityService = identityService;
		eventManager.register(this);
	}

	@IdenticEvent(EventOrder.LOW)
	public void onAccountClear(@NotNull AccountClearEvent event) {
		UUID connectionUniqueId = resolveConnectionUniqueId(event);
		if (connectionUniqueId == null)
			return;

		sessionService.close(connectionUniqueId).join();
	}

	private @Nullable UUID resolveConnectionUniqueId(@NotNull AccountClearEvent event) {
		UUID connectionUniqueId = event.getIdentity().getUniqueId();
		if (connectionUniqueId != null) return connectionUniqueId;

		String username = event.getIdentity().getUsername();
		if (username.isBlank()) return null;

		return identityService.find(username)
				.map(Identity::getUniqueId)
				.orElse(null);
	}
}
