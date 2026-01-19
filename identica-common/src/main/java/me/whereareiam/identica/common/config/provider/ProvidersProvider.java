package me.whereareiam.identica.common.config.provider;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import me.whereareiam.configura.Config;
import me.whereareiam.identica.Reloadable;
import me.whereareiam.identica.common.config.template.ProvidersTemplate;
import me.whereareiam.identica.model.config.Providers;
import me.whereareiam.identica.registry.Registry;

import java.nio.file.Path;

@Singleton
public class ProvidersProvider extends DefaultConfigProvider<Providers> {
	@Inject
	public ProvidersProvider(@Named("providersPath") Path providersPath, Registry<Reloadable> registry) {
		super(providersPath, registry);
	}

	@Override
	protected Providers load() {
		return Config.update(getBasePath().resolve("providers"), Providers.class);
	}

	@Override
	protected void registerTemplate() {
		Config.registerTemplate(ProvidersTemplate.class);
	}
}
