package me.whereareiam.identica.platform.adapter;

import me.whereareiam.identica.handshake.HandshakeApplierRegistry;
import me.whereareiam.identica.handshake.HandshakeContext;

/**
 * Required platform adapter role for applying platform-specific handshake
 * instructions.
 *
 * @param <C> handshake context type
 */
public interface PlatformHandshakeApplierRegistry<C extends HandshakeContext> extends HandshakeApplierRegistry<C> {
}
