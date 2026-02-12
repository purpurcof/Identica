package me.whereareiam.identica.handshake;

import com.google.inject.Provider;
import me.whereareiam.identica.Serializer;
import me.whereareiam.identica.model.auth.handshake.HandshakeDecision;
import me.whereareiam.identica.model.config.Messages;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Base class for applying handshake decisions in a platform-agnostic way.
 * Platform adapters should extend this class and provide the concrete decision targets.
 *
 * <pre>{@code
 * handshakeDecisionAdapter.apply(decision, target);
 * }</pre>
 */
public abstract class HandshakeDecisionAdapter {
	private final @NotNull Provider<Messages> messagesProvider;

	protected HandshakeDecisionAdapter(@NotNull Provider<Messages> messagesProvider) {
		this.messagesProvider = Objects.requireNonNull(messagesProvider, "messagesProvider");
	}

	/**
	 * Applies a handshake decision using the provided target.
	 *
	 * @param decision decision to apply
	 * @param target platform target for handshake actions
	 */
	public final void apply(
			@Nullable HandshakeDecision decision,
			@NotNull HandshakeDecisionTarget target
	) {
		if (decision == null || decision.getStatus() == null)
			return;

		if (decision.getStatus() == HandshakeDecision.Status.DENY) {
			target.deny(Serializer.serialize(resolveHandshakeMessage(decision.getMessage())));
		}
	}

	/**
	 * Resolves a decision message or falls back to the configured handshake denied messages.
	 *
	 * @param message decision message
	 * @return resolved message text
	 */
	protected @NotNull String resolveHandshakeMessage(@Nullable String message) {
		if (message != null && !message.isBlank())
			return message;

		return String.join("\n", messagesProvider.get().getConnection().getAuthentication().getHandshakeDenied());
	}

	/**
	 * Target abstraction for platform-specific handshake handling.
	 */
	public interface HandshakeDecisionTarget {
		/**
		 * Denies the handshake.
		 *
		 * @param message denial message
		 */
		void deny(@NotNull Component message);
	}
}
