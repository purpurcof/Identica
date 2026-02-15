package me.whereareiam.identica.provider.cracked.cryptography;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import me.whereareiam.identica.provider.cracked.CryptographyAlgorithm;
import me.whereareiam.identica.provider.cracked.CryptographyRegistry;

public class CryptographyModule extends AbstractModule {
	@Override
	protected void configure() {
		bind(CryptographyRegistry.class).to(DefaultCryptographyRegistry.class).asEagerSingleton();
		Multibinder.newSetBinder(binder(), CryptographyAlgorithm.class);
	}
}
