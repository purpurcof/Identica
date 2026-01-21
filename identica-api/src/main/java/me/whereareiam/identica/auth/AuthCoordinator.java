package me.whereareiam.identica.auth;

import me.whereareiam.identica.model.auth.AuthDecision;
import me.whereareiam.identica.model.auth.AuthContext;
import me.whereareiam.identica.model.auth.HandshakeDecision;
import me.whereareiam.identica.model.auth.HandshakeRequest;
import me.whereareiam.identica.model.auth.LoginRequest;
import me.whereareiam.identica.type.HandshakeMode;

import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

/**
 * Coordinates handshake and authentication flows across platforms.
 */
public interface AuthCoordinator {
	CompletionStage<HandshakeDecision> handshake(HandshakeRequest request);

	AuthDecision authenticate(LoginRequest request);

	AuthDecision resume(UUID connectionUniqueId);

	AuthDecision resume(UUID connectionUniqueId, Consumer<AuthContext> contextUpdater);

	boolean hasPending(UUID connectionUniqueId);

	boolean clearPending(UUID connectionUniqueId);

	void requestHandshakeDirective(String username, HandshakeMode mode);
}
