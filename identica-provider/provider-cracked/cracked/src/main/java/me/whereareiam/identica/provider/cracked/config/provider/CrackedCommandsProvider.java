package me.whereareiam.identica.provider.cracked.config.provider;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import me.whereareiam.configura.Config;
import me.whereareiam.identica.Reloadable;
import me.whereareiam.identica.provider.cracked.config.CrackedCommands;
import me.whereareiam.identica.provider.cracked.config.template.CrackedCommandsTemplate;
import me.whereareiam.identica.registry.Registry;

import java.nio.file.Path;

@Singleton
public class CrackedCommandsProvider extends CrackedConfigProvider<CrackedCommands> {
	@Inject
	public CrackedCommandsProvider(
			@Named("workingPath") Path workingPath,
			Registry<Reloadable> reloadables
	) {
		super(workingPath, reloadables);
	}

	@Override
	protected CrackedCommands load() {
		return Config.update(getBasePath().resolve("commands"), CrackedCommands.class);
	}

	@Override
	protected void registerTemplate() {
		Config.registerTemplate(CrackedCommandsTemplate.class);
	}
}
