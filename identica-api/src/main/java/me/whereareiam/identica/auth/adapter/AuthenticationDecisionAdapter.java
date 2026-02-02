package me.whereareiam.identica.auth.adapter;

import com.google.inject.Provider;
import me.whereareiam.identica.Serializer;
import me.whereareiam.identica.model.auth.AuthDecision;
import me.whereareiam.identica.model.config.Messages;
import me.whereareiam.keystone.Actor;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Base class for applying authentication decisions in a platform-agnostic way.
 * This is shared between login and resume listeners so both flows behave consistently.
 *
 * <pre>{@code
 * authenticationDecisionAdapter.apply(decision, actor, target);
 * }</pre>
 */
public abstract class AuthenticationDecisionAdapter {
	private final @NotNull Provider<Messages> messagesProvider;

	protected AuthenticationDecisionAdapter(@NotNull Provider<Messages> messagesProvider) {
		this.messagesProvider = Objects.requireNonNull(messagesProvider, "messagesProvider");
	}

	/**
	 * Applies an authentication decision using the provided actor and target.
	 *
	 * @param decision decision to apply
	 * @param actor actor receiving wait messages
	 * @param target platform target for deny/reconnect actions
	 */
	public final void apply(
			@Nullable AuthDecision decision,
			@NotNull Actor actor,
			@NotNull AuthenticationDecisionTarget target
	) {
		if (decision == null || decision.getStatus() == null)
			return;

		switch (decision.getStatus()) {
			case WAIT -> {
				String message = decision.getMessage();
				if (message == null || message.isBlank())
					return;

				actor.sendMessage(Serializer.serialize(actor, message));
			}
			case DENY -> target.deny(Serializer.serialize(actor, resolveAuthMessage(decision.getMessage())));
			case REQUIRE_RECONNECT -> target.requireReconnect(Serializer.serialize(actor, resolveAuthMessage(decision.getMessage())));
			default -> {
			}
		}
	}

	/**
	 * Resolves a decision message or falls back to the configured authentication failure messages.
	 *
	 * @param message decision message
	 * @return resolved message text
	 */
	protected @NotNull String resolveAuthMessage(@Nullable String message) {
		if (message != null && !message.isBlank())
			return message;

		return String.join("\n", messagesProvider.get().getAuthentication().getAuthenticationFailed());
	}

	/**
	 * Target abstraction for platform-specific denial and reconnect handling.
	 */
	public interface AuthenticationDecisionTarget {
		/**
		 * Denies the login attempt.
		 *
		 * @param message denial message
		 */
		void deny(@NotNull Component message);

		/**
		 * Requires the client to reconnect.
		 *
		 * @param message reconnect message
		 */
		void requireReconnect(@NotNull Component message);
	}
}
