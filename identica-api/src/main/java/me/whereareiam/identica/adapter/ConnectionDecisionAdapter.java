package me.whereareiam.identica.adapter;

import com.google.inject.Provider;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.Serializer;
import me.whereareiam.identica.model.auth.ConnectionDecision;
import me.whereareiam.identica.model.config.Messages;
import me.whereareiam.keystone.Actor;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Base class for applying connection decisions in a platform-agnostic way.
 * This is shared between login and resume listeners so both flows behave consistently.
 *
 * <pre>{@code
 * connectionDecisionAdapter.apply(decision, actor, target);
 * }</pre>
 */
@RequiredArgsConstructor
public abstract class ConnectionDecisionAdapter {
	private final @NotNull Provider<Messages> messagesProvider;

	/**
	 * Applies a connection decision using the provided actor and target.
	 *
	 * @param decision decision to apply
	 * @param actor    actor receiving wait messages
	 * @param target   platform target for deny/reconnect actions
	 */
	public final void apply(
			@Nullable ConnectionDecision decision,
			@NotNull Actor actor,
			@NotNull ConnectionDecisionTarget target
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

		return String.join("\n", messagesProvider.get().getConnection().getAuthentication().getAuthenticationFailed());
	}

	/**
	 * Target abstraction for platform-specific denial and reconnect handling.
	 */
	public interface ConnectionDecisionTarget {
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
