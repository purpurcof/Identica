package me.whereareiam.identica.provider.premium.platform.velocity;

import com.google.inject.AbstractModule;
import me.whereareiam.identica.provider.premium.platform.velocity.handshake.PremiumHandshakeApplier;
import me.whereareiam.identica.provider.premium.platform.velocity.handshake.PremiumVelocityHandshakeApplierLifecycle;
import me.whereareiam.identica.provider.premium.platform.velocity.listener.connection.PremiumGameProfileRequestListener;

public class PremiumVelocityModule extends AbstractModule {
	@Override
	protected void configure() {
		bind(PremiumGameProfileRequestListener.class).asEagerSingleton();
		bind(PremiumHandshakeApplier.class).asEagerSingleton();
		bind(PremiumVelocityHandshakeApplierLifecycle.class).asEagerSingleton();
	}
}
