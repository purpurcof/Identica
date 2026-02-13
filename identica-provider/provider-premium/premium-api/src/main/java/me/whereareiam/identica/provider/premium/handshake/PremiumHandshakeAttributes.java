package me.whereareiam.identica.provider.premium.handshake;

import me.whereareiam.identica.handshake.HandshakeAttributeKey;

/**
 * Typed handshake attributes used by the premium provider.
 */
public final class PremiumHandshakeAttributes {
	public static final HandshakeAttributeKey<PremiumForceOnlineInstruction> FORCE_ONLINE =
			HandshakeAttributeKey.json("premium:force-online", PremiumForceOnlineInstruction.class);

	private PremiumHandshakeAttributes() {
	}
}
