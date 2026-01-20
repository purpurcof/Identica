package me.whereareiam.identica.provider.cracked.config.provider;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import me.whereareiam.configura.Config;
import me.whereareiam.identica.Reloadable;
import me.whereareiam.identica.provider.cracked.config.CrackedSettings;
import me.whereareiam.identica.provider.cracked.config.template.CrackedSettingsTemplate;
import me.whereareiam.identica.registry.Registry;

import java.nio.file.Path;

@Singleton
public class CrackedSettingsProvider extends CrackedConfigProvider<CrackedSettings> {
	@Inject
	public CrackedSettingsProvider(
			@Named("workingPath") Path workingPath,
			Registry<Reloadable> reloadables
	) {
		super(workingPath, reloadables);
	}

	@Override
	protected CrackedSettings load() {
		return Config.update(getBasePath().resolve("config"), CrackedSettings.class);
	}

	@Override
	protected void registerTemplate() {
		Config.registerTemplate(CrackedSettingsTemplate.class);
	}
}
