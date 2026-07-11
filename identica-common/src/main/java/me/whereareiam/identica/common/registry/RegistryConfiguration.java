package me.whereareiam.identica.common.registry;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import me.whereareiam.identica.Registry;
import me.whereareiam.identica.Reloadable;

import java.util.Set;

public class RegistryConfiguration extends AbstractModule {
	@Override
	protected void configure() {
		bind(new TypeLiteral<Registry<Reloadable>>() {})
				.to(ReloadableRegistry.class)
				.asEagerSingleton();
		bind(new TypeLiteral<Set<Reloadable>>() {})
				.annotatedWith(Names.named("reloadables"))
				.toProvider(ReloadableRegistry.class)
				.asEagerSingleton();
	}
}
