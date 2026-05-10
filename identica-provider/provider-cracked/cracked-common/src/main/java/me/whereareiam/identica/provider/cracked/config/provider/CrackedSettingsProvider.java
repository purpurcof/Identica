package me.whereareiam.identica.provider.cracked.config.provider;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import me.whereareiam.identica.Reloadable;
import me.whereareiam.identica.Registry;
import me.whereareiam.identica.config.ConfigProvider;
import me.whereareiam.identica.provider.cracked.config.CrackedSettings;
import me.whereareiam.identica.provider.cracked.config.template.CrackedSettingsTemplate;

import java.nio.file.Path;

@Singleton
public class CrackedSettingsProvider extends ConfigProvider<CrackedSettings> {
	@Inject
	public CrackedSettingsProvider(
			@Named("workingPath") Path workingPath,
			Registry<Reloadable> reloadables
	) {
		super(
				workingPath,
				"settings",
				CrackedSettings.class,
				reloadables,
				configure(CrackedSettingsTemplate.class, CrackedSettings.class)
		);
	}
}
