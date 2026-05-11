package me.whereareiam.identica.provider.cracked.config.provider;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import me.whereareiam.identica.Reloadable;
import me.whereareiam.identica.Registry;
import me.whereareiam.identica.config.ConfigProvider;
import me.whereareiam.identica.provider.cracked.config.CrackedCommands;
import me.whereareiam.identica.provider.cracked.config.defaults.CrackedCommandsDefaults;

import java.nio.file.Path;

@Singleton
public class CrackedCommandsProvider extends ConfigProvider<CrackedCommands> {
	@Inject
	public CrackedCommandsProvider(
			@Named("workingPath") Path workingPath,
			Registry<Reloadable> reloadables
	) {
		super(
				workingPath,
				"commands",
				CrackedCommands.class,
				reloadables,
				configure(CrackedCommandsDefaults.class, CrackedCommands.class)
		);
	}
}
