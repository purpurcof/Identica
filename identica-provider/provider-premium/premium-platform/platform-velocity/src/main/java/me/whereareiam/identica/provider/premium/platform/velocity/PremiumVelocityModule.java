package me.whereareiam.identica.provider.premium.platform.velocity;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import me.whereareiam.identica.provider.ProviderPlatformBinding;
import me.whereareiam.identica.provider.premium.platform.velocity.handshake.PremiumHandshakeApplier;
import me.whereareiam.identica.provider.premium.platform.velocity.handshake.PremiumVelocityHandshakeBinding;
import me.whereareiam.identica.provider.premium.platform.velocity.listener.connection.PremiumGameProfileRequestListener;

public class PremiumVelocityModule extends AbstractModule {
	@Override
	protected void configure() {
		bind(PremiumGameProfileRequestListener.class).asEagerSingleton();
		bind(PremiumHandshakeApplier.class).asEagerSingleton();
		bind(PremiumVelocityHandshakeBinding.class).asEagerSingleton();
		Multibinder.newSetBinder(binder(), ProviderPlatformBinding.class)
				.addBinding()
				.to(PremiumVelocityHandshakeBinding.class);
	}
}
