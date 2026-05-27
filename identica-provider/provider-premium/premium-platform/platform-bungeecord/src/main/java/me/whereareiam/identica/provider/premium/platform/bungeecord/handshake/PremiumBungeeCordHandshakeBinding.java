package me.whereareiam.identica.provider.premium.platform.bungeecord.handshake;

import com.google.inject.Inject;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.platform.adapter.PlatformHandshakeApplierRegistry;
import me.whereareiam.identica.platform.bungeecord.api.handshake.BungeeCordHandshakeContext;
import me.whereareiam.identica.provider.ProviderPlatformBinding;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public final class PremiumBungeeCordHandshakeBinding implements ProviderPlatformBinding {
	private final PlatformHandshakeApplierRegistry<BungeeCordHandshakeContext> applierRegistry;
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
