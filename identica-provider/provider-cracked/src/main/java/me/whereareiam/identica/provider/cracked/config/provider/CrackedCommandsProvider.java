package me.whereareiam.identica.provider.cracked.config.provider;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import me.whereareiam.configura.Config;
import me.whereareiam.identica.Reloadable;
import me.whereareiam.identica.model.config.Commands;
import me.whereareiam.identica.provider.cracked.config.template.CrackedCommandsTemplate;
import me.whereareiam.identica.registry.Registry;

import java.nio.file.Path;

@Singleton
public class CrackedCommandsProvider extends CrackedConfigProvider<Commands> {
	@Inject
	public CrackedCommandsProvider(@Named("providersPath") Path providersPath, Registry<Reloadable> reloadables) {
		super(providersPath.resolve("Cracked"), reloadables);
	}

	@Override
	protected Commands load() {
		return Config.update(getBasePath().resolve("commands"), Commands.class);
	}

	@Override
	protected void registerTemplate() {
		Config.registerTemplate(CrackedCommandsTemplate.class);
	}
}
