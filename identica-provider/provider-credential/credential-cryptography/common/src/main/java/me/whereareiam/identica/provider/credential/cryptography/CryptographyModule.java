package me.whereareiam.identica.provider.credential.cryptography;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import me.whereareiam.identica.provider.credential.CryptographyAlgorithm;
import me.whereareiam.identica.provider.credential.CryptographyRegistry;

public class CryptographyModule extends AbstractModule {
	@Override
	protected void configure() {
		bind(CryptographyRegistry.class).to(DefaultCryptographyRegistry.class).asEagerSingleton();
		bind(CryptographyService.class).to(DefaultCryptographyService.class).asEagerSingleton();
		Multibinder.newSetBinder(binder(), CryptographyAlgorithm.class);
	}
}
