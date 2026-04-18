package me.whereareiam.identica.handshake;

import com.google.inject.Provider;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.ConnectionCoordinator;
import me.whereareiam.identica.Serializer;
import me.whereareiam.identica.pipeline.prepare.PrepareStateStore;
import me.whereareiam.identica.identity.actor.ConnectionIdentity;
import me.whereareiam.identica.logging.Logger;
import me.whereareiam.identica.model.auth.handshake.HandshakeDecision;
import me.whereareiam.identica.model.auth.handshake.HandshakeInstruction;
import me.whereareiam.identica.model.pipeline.prepare.decision.PrepareDecision;
import me.whereareiam.identica.model.pipeline.prepare.PrepareRequest;
import me.whereareiam.identica.type.PrepareStage;
import me.whereareiam.identica.model.config.Messages;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletionStage;

/**
 * Base class for applying handshake decisions in a platform-agnostic way.
 * Platform adapters should extend this class and provide the concrete decision targets.
 *
 * <pre>{@code
 * handshakeDecisionAdapter.apply(decision, target);
 * }</pre>
 */
@RequiredArgsConstructor
public abstract class HandshakeDecisionAdapter {
	private final @NotNull ConnectionCoordinator connectionCoordinator;
	private final @NotNull PrepareStateStore prepareStateStore;
	private final @NotNull HandshakeStore handshakeStore;
	private final @NotNull Provider<Messages> messagesProvider;

	/**
	 * Runs the shared handshake preparation flow and applies the resolved platform target.
	 *
	 * @param request platform-neutral handshake request
	 * @param target platform target for handshake actions
	 * @return completion stage that finishes once the flow has been applied
	 */
	protected final @NotNull CompletionStage<Void> adapt(
			@NotNull HandshakeAdapterRequest request,
			@NotNull HandshakeDecisionTarget target
	) {
		PrepareRequest prepareRequest = PrepareRequest.builder()
				.stage(PrepareStage.HANDSHAKE)
				.identity(request.identity())
				.build();

		return connectionCoordinator.prepare(prepareRequest)
				.handle((decision, error) -> {
					if (error != null) {
						Logger.severe("Prepare handshake failed %s", error);
						return null;
					}

					PrepareDecision prepared = decision != null ? decision : PrepareDecision.allow();
					storePreparedState(prepareRequest.getConnectionKey(), prepared);

					HandshakeDecision resolved = resolveHandshakeDecision(prepared);
					apply(resolved, target);
					applyHandshakeInstruction(request, prepared);

					return null;
				});
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
		if (decision == null || decision.getStatus() == null) return;

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
		if (message != null && !message.isBlank()) return message;
		return String.join("\n", messagesProvider.get().getConnection().getPrepare().getHandshakeDenied());
	}

	private void storePreparedState(@Nullable String connectionKey, @NotNull PrepareDecision prepared) {
		if (connectionKey == null || connectionKey.isBlank()) return;
		prepareStateStore.put(connectionKey, prepared);
	}

	private @NotNull HandshakeDecision resolveHandshakeDecision(@NotNull PrepareDecision prepared) {
		if (prepared.getHandshake() != null) return prepared.getHandshake();
		if (prepared.isDenied()) return HandshakeDecision.deny(prepared.getDenialMessage());

		return HandshakeDecision.allow();
	}

	private void applyHandshakeInstruction(
			@NotNull HandshakeAdapterRequest request,
			@NotNull PrepareDecision prepared
	) {
		if (prepared.isDenied()) return;

		ConnectionIdentity identity = request.identity();
		String ip = identity.getIp();
		if (ip == null || ip.isBlank()) return;

		handshakeStore.consumeInstruction(identity.getUsername(), ip)
				.ifPresent(request.instructionTarget()::apply);
	}

	/**
	 * Platform-neutral handshake request used by the shared adaptation flow.
	 *
	 * @param identity normalized connection identity
	 * @param instructionTarget platform-specific instruction application target
	 */
	public record HandshakeAdapterRequest(
			@NotNull ConnectionIdentity identity,
			@NotNull HandshakeInstructionTarget instructionTarget
	) {
	}

	/**
	 * Target abstraction for platform-specific handshake instruction handling.
	 */
	public interface HandshakeInstructionTarget {
		void apply(@NotNull HandshakeInstruction instruction);
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
