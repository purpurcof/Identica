package me.whereareiam.identica.provider.premium.config.provider;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import me.whereareiam.configura.Config;
import me.whereareiam.identica.Reloadable;
import me.whereareiam.identica.model.config.Commands;
import me.whereareiam.identica.provider.premium.config.template.PremiumCommandsTemplate;
import me.whereareiam.identica.registry.Registry;

import java.nio.file.Path;

@Singleton
public class PremiumCommandsProvider extends PremiumConfigProvider<Commands> {
	@Inject
	public PremiumCommandsProvider(
			@Named("workingPath") Path workingPath,
			Registry<Reloadable> reloadables
	) {
		super(workingPath, reloadables);
	}

	@Override
	protected Commands load() {
		return Config.update(getBasePath().resolve("commands"), Commands.class);
	}

	@Override
	protected void registerTemplate() {
		Config.registerTemplate(PremiumCommandsTemplate.class);
	}
}
