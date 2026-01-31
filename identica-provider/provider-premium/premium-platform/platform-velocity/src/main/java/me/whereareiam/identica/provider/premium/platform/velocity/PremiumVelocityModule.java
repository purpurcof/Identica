package me.whereareiam.identica.provider.premium.platform.velocity;

import com.google.inject.AbstractModule;
import me.whereareiam.identica.provider.premium.platform.velocity.listener.connection.PremiumGameProfileRequestListener;

public class PremiumVelocityModule extends AbstractModule {
	@Override
	protected void configure() {
		bind(PremiumGameProfileRequestListener.class).asEagerSingleton();
	}
}
