package me.whereareiam.identica.handshake;

import me.whereareiam.identica.model.auth.handshake.HandshakeDecision;
import me.whereareiam.identica.model.auth.handshake.HandshakeRequest;

import java.util.concurrent.CompletionStage;

/**
 * Evaluates handshake requests and returns a decision asynchronously.
 */
public interface HandshakePolicy {
	CompletionStage<HandshakeDecision> evaluate(HandshakeRequest request);
}
