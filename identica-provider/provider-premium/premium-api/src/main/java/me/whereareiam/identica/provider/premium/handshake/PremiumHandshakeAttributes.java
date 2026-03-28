package me.whereareiam.identica.provider.premium.handshake;

import me.whereareiam.identica.model.HandshakeAttributeKey;

/**
 * Typed handshake attributes used by the premium provider.
 */
public final class PremiumHandshakeAttributes {
	public static final HandshakeAttributeKey<Boolean> FORCE_ONLINE = HandshakeAttributeKey.bool("premium:force-online");
}
