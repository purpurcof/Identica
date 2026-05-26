package me.whereareiam.identica.provider.premium.platform.bungeecord;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import me.whereareiam.identica.provider.ProviderPlatformBinding;
import me.whereareiam.identica.provider.premium.platform.bungeecord.handshake.PremiumBungeeCordHandshakeBinding;
import me.whereareiam.identica.provider.premium.platform.bungeecord.handshake.PremiumHandshakeApplier;
import me.whereareiam.identica.provider.premium.platform.bungeecord.listener.connection.PremiumLoginListener;
import me.whereareiam.identica.provider.premium.platform.bungeecord.listener.connection.PremiumPostLoginListener;

public class PremiumBungeeCordModule extends AbstractModule {
	@Override
	protected void configure() {
		bind(PremiumLoginListener.class).asEagerSingleton();
		bind(PremiumPostLoginListener.class).asEagerSingleton();
		bind(PremiumHandshakeApplier.class).asEagerSingleton();
		bind(PremiumBungeeCordHandshakeBinding.class).asEagerSingleton();
		Multibinder.newSetBinder(binder(), ProviderPlatformBinding.class)
				.addBinding()
				.to(PremiumBungeeCordHandshakeBinding.class);
	}
}
