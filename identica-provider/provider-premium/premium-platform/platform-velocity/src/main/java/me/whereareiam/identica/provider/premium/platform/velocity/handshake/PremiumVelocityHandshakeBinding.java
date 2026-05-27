package me.whereareiam.identica.provider.premium.platform.velocity.handshake;

import com.google.inject.Inject;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.platform.adapter.PlatformHandshakeApplierRegistry;
import me.whereareiam.identica.platform.velocity.api.handshake.VelocityHandshakeContext;
import me.whereareiam.identica.provider.ProviderPlatformBinding;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public final class PremiumVelocityHandshakeBinding implements ProviderPlatformBinding {
	private final PlatformHandshakeApplierRegistry<VelocityHandshakeContext> applierRegistry;
	private final PremiumHandshakeApplier applier;

	@Override
	public void register() {
		applierRegistry.register(applier);
	}

	@Override
	public void unregister() {
		applierRegistry.unregister(applier);
	}
}
