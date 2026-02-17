package me.whereareiam.identica.provider.premium.handshake;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Payload used to request a premium online-mode handshake.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PremiumForceOnlineInstruction {
	private String reason;
}
