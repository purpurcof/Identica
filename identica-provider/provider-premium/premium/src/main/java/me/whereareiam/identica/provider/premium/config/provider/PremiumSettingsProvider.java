package me.whereareiam.identica.provider.premium.config.provider;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import me.whereareiam.identica.Reloadable;
import me.whereareiam.identica.Registry;
import me.whereareiam.identica.config.ConfigProvider;
import me.whereareiam.identica.provider.premium.config.PremiumSettings;
import me.whereareiam.identica.provider.premium.config.defaults.PremiumSettingsDefaults;

import java.nio.file.Path;

@Singleton
public class PremiumSettingsProvider extends ConfigProvider<PremiumSettings> {
	@Inject
	public PremiumSettingsProvider(
			@Named("workingPath") Path workingPath,
			Registry<Reloadable> reloadables
	) {
		super(
				workingPath,
				"settings",
				PremiumSettings.class,
				reloadables,
				configure(PremiumSettingsDefaults.class, PremiumSettings.class)
		);
	}
}
