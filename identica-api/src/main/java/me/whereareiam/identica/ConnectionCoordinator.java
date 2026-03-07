package me.whereareiam.identica;

import me.whereareiam.identica.model.auth.ConnectionDecision;
import me.whereareiam.identica.model.auth.request.ConnectionRequest;
import me.whereareiam.identica.model.auth.request.AdvanceRequest;
import me.whereareiam.identica.model.auth.request.ResumeRequest;
import me.whereareiam.identica.model.prepare.PrepareDecision;
import me.whereareiam.identica.model.prepare.PrepareRequest;
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
	 * Prepares a connection before the main scenario pipelines run.
	 *
	 * <p>This generic preparation flow may include early handshake evaluation,
	 * provider resolution, profile rewriting, and conflict handling depending on
	 * the requested stage.</p>
	 *
	 * @param request preparation request details, or {@code null} when unavailable
	 * @return a completion stage that resolves to the preparation decision
	 */
	@NotNull
	CompletionStage<PrepareDecision> prepare(@Nullable PrepareRequest request);

	/**
	 * Processes a connection using the provided request asynchronously.
	 *
	 * @param request connection request details, or {@code null} when unavailable
	 * @return a completion stage that resolves to the connection decision
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
	 * @return a completion stage that resolves to the connection decision
	 */
	@NotNull
	CompletionStage<ConnectionDecision> resume(
			@NotNull ResumeRequest request
	);

	/**
	 * Advances a pending connection flow within the same session.
	 *
	 * @param request advance request details
	 * @return a completion stage that resolves to the connection decision
	 */
	@NotNull
	CompletionStage<ConnectionDecision> advance(
			@NotNull AdvanceRequest request
	);

	/**
	 * Checks whether a connection has a pending flow.
	 *
	 * @param connectionUniqueId unique connection identifier
	 * @return {@code true} if the connection has a pending flow
	 */
	boolean hasPending(@NotNull UUID connectionUniqueId);

}
