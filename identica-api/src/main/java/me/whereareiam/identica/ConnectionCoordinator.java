package me.whereareiam.identica;

import me.whereareiam.identica.model.auth.ConnectionDecision;
import me.whereareiam.identica.model.auth.handshake.HandshakeDecision;
import me.whereareiam.identica.model.auth.handshake.HandshakeRequest;
import me.whereareiam.identica.model.auth.request.ConnectionRequest;
import me.whereareiam.identica.model.auth.request.ProfileRequest;
import me.whereareiam.identica.model.auth.request.ResumeRequest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.CompletionStage;

/**
 * Public API for coordinating connection flows and handshake decisions.
 *
 * <p>This service exposes high-level connection operations that return {@link ConnectionDecision}
 * results. It delegates flow execution to Identica's internal pipeline and is safe to use by
 * platform adapters and external plugins.</p>
 *
 * <p><b>Example:</b></p>
 * <pre>{@code
 * ConnectionCoordinator connectionCoordinator = IdenticaAPI.getConnectionCoordinator();
 * ConnectionDecision decision = connectionCoordinator.process(connectionRequest).toCompletableFuture().join();
 * if (decision.getStatus().isDenied()) {
 *     // handle denial
 * }
 * }</pre>
 */
@SuppressWarnings("unused")
public interface ConnectionCoordinator {
	/**
	 * Evaluates a handshake request and returns the decision asynchronously.
	 *
	 * @param request handshake request details, or {@code null} when unavailable
	 * @return a completion journey that resolves to the handshake decision
	 */
	@NotNull
	CompletionStage<HandshakeDecision> handshake(@Nullable HandshakeRequest request);

	/**
	 * Prepares a resolver UUID for a connection using the provided request data.
	 *
	 * @param request resolver rewrite request details, or {@code null} when unavailable
	 * @return the resolved Identica UUID, or {@code null} when it cannot be resolved
	 */
	@Nullable
	UUID prepareProfile(@Nullable ProfileRequest request);

	/**
	 * Processes a connection using the provided request asynchronously.
	 *
	 * @param request connection request details, or {@code null} when unavailable
	 * @return a completion journey that resolves to the connection decision
	 */
	@NotNull
	CompletionStage<ConnectionDecision> process(@Nullable ConnectionRequest request);

	/**
	 * Resumes a pending connection flow.
	 *
	 * <p>The resume request should include the latest connection info so any
	 * platform-specific handshake requirements can be evaluated correctly.</p>
	 *
	 * @param request resume request details
	 * @return a completion journey that resolves to the connection decision
	 */
	@NotNull
	CompletionStage<ConnectionDecision> resume(
			@NotNull ResumeRequest request
	);

	/**
	 * Checks whether a connection has a pending flow.
	 *
	 * @param connectionUniqueId unique connection identifier
	 * @return {@code true} if the connection has a pending flow
	 */
	boolean hasPending(@NotNull UUID connectionUniqueId);

	/**
	 * Clears any pending flow for the provided connection ID.
	 *
	 * @param connectionUniqueId unique connection identifier
	 * @return {@code true} if the pending flow was cleared
	 */
	boolean clearPending(@NotNull UUID connectionUniqueId);

}
