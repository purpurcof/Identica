package me.whereareiam.identica.provider.credential.cryptography.argon2;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import me.whereareiam.identica.provider.credential.CryptographyAlgorithm;

public class Argon2CryptographyModule extends AbstractModule {
	@Override
	protected void configure() {
		Multibinder.newSetBinder(binder(), CryptographyAlgorithm.class)
				.addBinding()
				.to(Argon2CryptographyAlgorithm.class);
	}
}
