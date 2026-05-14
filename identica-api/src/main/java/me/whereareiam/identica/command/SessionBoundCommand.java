package me.whereareiam.identica.command;

import me.whereareiam.identica.identity.actor.Identity;
import me.whereareiam.identica.identity.session.SessionService;
import me.whereareiam.identica.model.Session;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public abstract class SessionBoundCommand extends IdentityCommand {
	protected final @Nullable Session requireCurrentSession(@NotNull Identity identity) {
		UUID accountUniqueId = requireAccountUniqueId(identity);
		if (accountUniqueId == null) {
			sendMessageText(identity, currentSessionRequiredMessage());
			return null;
		}

		Session session = sessionService().findByUniqueId(accountUniqueId).join().orElse(null);
		if (session != null) return session;

		sendMessageText(identity, currentSessionRequiredMessage());
		return null;
	}

	protected final @Nullable UUID requireAccountUniqueId(@NotNull Identity identity) {
		UUID accountUniqueId = identity.getAccountUniqueId();
		if (accountUniqueId != null) return accountUniqueId;

		sendMessageText(identity, currentSessionRequiredMessage());
		return null;
	}

	protected abstract @NotNull SessionService sessionService();

	protected abstract @Nullable String currentSessionRequiredMessage();
}
