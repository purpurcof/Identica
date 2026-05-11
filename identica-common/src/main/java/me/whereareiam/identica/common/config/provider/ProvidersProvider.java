package me.whereareiam.identica.common.config.provider;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import me.whereareiam.identica.Reloadable;
import me.whereareiam.identica.common.config.defaults.ProvidersDefaults;
import me.whereareiam.identica.config.ConfigProvider;
import me.whereareiam.identica.model.config.Providers;
import me.whereareiam.identica.Registry;

import java.nio.file.Path;

@Singleton
public class ProvidersProvider extends ConfigProvider<Providers> {
	@Inject
	public ProvidersProvider(
			@Named("providersPath") Path providersPath,
			Registry<Reloadable> registry
	) {
		super(providersPath, "providers", Providers.class, registry, configure(ProvidersDefaults.class, Providers.class));
	}
}
