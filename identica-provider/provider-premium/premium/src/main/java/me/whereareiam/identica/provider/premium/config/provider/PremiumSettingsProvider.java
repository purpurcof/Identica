package me.whereareiam.identica.provider.premium.config.provider;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import me.whereareiam.configura.Config;
import me.whereareiam.identica.Reloadable;
import me.whereareiam.identica.provider.premium.config.PremiumSettings;
import me.whereareiam.identica.provider.premium.config.template.PremiumSettingsTemplate;
import me.whereareiam.identica.registry.Registry;

import java.nio.file.Path;

@Singleton
public class PremiumSettingsProvider extends PremiumConfigProvider<PremiumSettings> {
	@Inject
	public PremiumSettingsProvider(
			@Named("workingPath") Path workingPath,
			Registry<Reloadable> reloadables
	) {
		super(workingPath, reloadables);
	}

	@Override
	protected PremiumSettings load() {
		return Config.update(getBasePath().resolve("config"), PremiumSettings.class);
	}

	@Override
	protected void registerTemplate() {
		Config.registerTemplate(PremiumSettingsTemplate.class);
	}
}
