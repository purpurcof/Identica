package me.whereareiam.identica.provider.premium.platform.velocity.handshake;

import com.velocitypowered.api.event.connection.PreLoginEvent;
import me.whereareiam.identica.handshake.HandshakeApplier;
import me.whereareiam.identica.model.auth.handshake.HandshakeInstruction;
import me.whereareiam.identica.platform.velocity.api.handshake.VelocityHandshakeContext;
import me.whereareiam.identica.provider.premium.handshake.PremiumHandshakeAttributes;
import org.jetbrains.annotations.NotNull;

public class PremiumHandshakeApplier implements HandshakeApplier<VelocityHandshakeContext> {
	@Override
	public void apply(@NotNull VelocityHandshakeContext context, @NotNull HandshakeInstruction instruction) {
		if (instruction.getAttribute(PremiumHandshakeAttributes.FORCE_ONLINE).isEmpty())
			return;

		PreLoginEvent event = context.event();
		event.setResult(PreLoginEvent.PreLoginComponentResult.forceOnlineMode());
	}
}
