package me.whereareiam.identica.command;

import me.whereareiam.identica.identity.actor.Identity;
import me.whereareiam.identica.identity.session.SessionService;
import me.whereareiam.identica.model.Session;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class SessionBoundCommand extends IdentityCommand {
	protected final @Nullable Session requireCurrentSession(@NotNull Identity identity) {
		Session session = sessionService().findByUniqueId(identity.getUniqueId()).join().orElse(null);
		if (session != null) return session;

		sendMessageText(identity, currentSessionRequiredMessage());
		return null;
	}

	protected abstract @NotNull SessionService sessionService();

	protected abstract @Nullable String currentSessionRequiredMessage();
}
