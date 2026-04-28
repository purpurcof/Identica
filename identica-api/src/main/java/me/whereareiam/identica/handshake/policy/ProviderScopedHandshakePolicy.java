package me.whereareiam.identica.handshake.policy;

import org.jetbrains.annotations.NotNull;

/**
 * Handshake policy bound to a specific provider id.
 */
public interface ProviderScopedHandshakePolicy extends HandshakePolicy {
	@NotNull String providerId();
}
