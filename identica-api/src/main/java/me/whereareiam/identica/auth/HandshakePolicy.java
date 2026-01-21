package me.whereareiam.identica.auth;

import me.whereareiam.identica.model.auth.HandshakeDecision;
import me.whereareiam.identica.model.auth.HandshakeRequest;

import java.util.concurrent.CompletionStage;

/**
 * Evaluates handshake requests and returns a decision asynchronously.
 */
public interface HandshakePolicy {
	CompletionStage<HandshakeDecision> evaluate(HandshakeRequest request);
}
