package me.whereareiam.identica.auth;

import me.whereareiam.identica.model.auth.AuthDecision;
import me.whereareiam.identica.model.auth.AuthContext;
import me.whereareiam.identica.model.auth.handshake.HandshakeDecision;
import me.whereareiam.identica.model.auth.handshake.HandshakeRequest;
import me.whereareiam.identica.model.auth.request.LoginRequest;
import me.whereareiam.identica.model.auth.request.ProfileRequest;
import me.whereareiam.identica.type.HandshakeMode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

/**
 * Coordinates handshake and authentication flows across platforms.
 */
@SuppressWarnings("unused")
public interface AuthCoordinator {
	/**
	 * Evaluates the handshake request and returns a decision for the connection.
	 *
	 * @param request handshake request details, or {@code null} when unavailable.
	 * @return a completion stage that resolves to the handshake decision.
	 */
	@NotNull
	CompletionStage<HandshakeDecision> handshake(@Nullable HandshakeRequest request);

	/**
	 * Prepares a profile UUID using connection-scoped data.
	 *
	 * @param request profile rewrite request details, or {@code null} when unavailable.
	 * @return the resolved Identica UUID, or {@code null} when it cannot be resolved.
	 */
	@Nullable
	UUID prepareProfile(@Nullable ProfileRequest request);

	/**
	 * Authenticates a connection using the provided login request.
	 *
	 * @param request login request details, or {@code null} when unavailable.
	 * @return the authentication decision for the connection.
	 */
	@NotNull
	AuthDecision authenticate(@Nullable LoginRequest request);

	/**
	 * Resumes a pending authentication flow for the provided connection ID.
	 *
	 * @param connectionUniqueId unique connection identifier.
	 * @return the authentication decision for the resumed connection.
	 */
	@NotNull
	AuthDecision resume(@NotNull UUID connectionUniqueId);

	/**
	 * Resumes a pending authentication flow with an optional context updater.
	 *
	 * @param connectionUniqueId unique connection identifier.
	 * @param contextUpdater optional callback to mutate the authentication context.
	 * @return the authentication decision for the resumed connection.
	 */
	@NotNull
	AuthDecision resume(@NotNull UUID connectionUniqueId, @Nullable Consumer<AuthContext> contextUpdater);

	/**
	 * Checks whether a connection has a pending authentication flow.
	 *
	 * @param connectionUniqueId unique connection identifier.
	 * @return {@code true} if the connection has a pending flow.
	 */
	boolean hasPending(@NotNull UUID connectionUniqueId);

	/**
	 * Clears any pending authentication flow for the provided connection ID.
	 *
	 * @param connectionUniqueId unique connection identifier.
	 * @return {@code true} if the pending flow was cleared.
	 */
	boolean clearPending(@NotNull UUID connectionUniqueId);

	/**
	 * Requests that a handshake instruction be stored for a given username.
	 *
	 * @param username username to associate with the instruction.
	 * @param mode requested handshake mode.
	 */
	void requestHandshakeInstruction(@Nullable String username, @Nullable HandshakeMode mode);
}
