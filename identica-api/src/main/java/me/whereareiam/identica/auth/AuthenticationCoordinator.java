package me.whereareiam.identica.auth;

import me.whereareiam.identica.model.auth.AuthContext;
import me.whereareiam.identica.model.auth.AuthDecision;
import me.whereareiam.identica.model.auth.handshake.HandshakeDecision;
import me.whereareiam.identica.model.auth.handshake.HandshakeRequest;
import me.whereareiam.identica.model.auth.request.LoginRequest;
import me.whereareiam.identica.model.auth.request.ProfileRequest;
import me.whereareiam.identica.model.auth.request.ResumeRequest;
import me.whereareiam.identica.type.HandshakeMode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

/**
 * Public API for coordinating authentication flows and handshake decisions.
 *
 * <p>This service exposes high-level authentication operations that return {@link AuthDecision}
 * results. It delegates flow execution to Identica's internal pipeline and is safe to use by
 * platform adapters and external plugins.</p>
 *
 * <p><b>Example:</b></p>
 * <pre>{@code
 * AuthenticationCoordinator authService = IdenticaAPI.getAuthService();
 * AuthDecision decision = authService.authenticate(loginRequest).toCompletableFuture().join();
 * if (decision.getStatus().isDenied()) {
 *     // handle denial
 * }
 * }</pre>
 */
@SuppressWarnings("unused")
public interface AuthenticationCoordinator {
	/**
	 * Evaluates a handshake request and returns the decision asynchronously.
	 *
	 * @param request handshake request details, or {@code null} when unavailable
	 * @return a completion stage that resolves to the handshake decision
	 */
	@NotNull
	CompletionStage<HandshakeDecision> handshake(@Nullable HandshakeRequest request);

	/**
	 * Prepares a profile UUID for a connection using the provided request data.
	 *
	 * @param request profile rewrite request details, or {@code null} when unavailable
	 * @return the resolved Identica UUID, or {@code null} when it cannot be resolved
	 */
	@Nullable
	UUID prepareProfile(@Nullable ProfileRequest request);

	/**
	 * Authenticates a connection using the provided login request asynchronously.
	 *
	 * @param request login request details, or {@code null} when unavailable
	 * @return a completion stage that resolves to the authentication decision
	 */
	@NotNull
	CompletionStage<AuthDecision> authenticate(@Nullable LoginRequest request);

	/**
	 * Resumes a pending authentication flow with an optional context updater.
	 *
	 * <p>The resume request should include the latest connection info to ensure
	 * online/offline requirements are evaluated correctly.</p>
	 *
	 * @param request resume request details
	 * @param contextUpdater optional callback to mutate the authentication context
	 * @return a completion stage that resolves to the authentication decision
	 */
	@NotNull
	CompletionStage<AuthDecision> resume(
			@NotNull ResumeRequest request,
			@Nullable Consumer<AuthContext> contextUpdater
	);

	/**
	 * Checks whether a connection has a pending authentication flow.
	 *
	 * @param connectionUniqueId unique connection identifier
	 * @return {@code true} if the connection has a pending flow
	 */
	boolean hasPending(@NotNull UUID connectionUniqueId);

	/**
	 * Clears any pending authentication flow for the provided connection ID.
	 *
	 * @param connectionUniqueId unique connection identifier
	 * @return {@code true} if the pending flow was cleared
	 */
	boolean clearPending(@NotNull UUID connectionUniqueId);

	/**
	 * Requests that a handshake instruction be stored for a given username.
	 *
	 * @param username username to associate with the instruction
	 * @param mode requested handshake mode
	 */
	void requestHandshakeInstruction(@Nullable String username, @Nullable HandshakeMode mode);
}
