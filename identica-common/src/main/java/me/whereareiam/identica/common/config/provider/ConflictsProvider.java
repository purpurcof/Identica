package me.whereareiam.identica.common.config.provider;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import me.whereareiam.identica.Reloadable;
import me.whereareiam.identica.Registry;
import me.whereareiam.identica.common.config.defaults.provider.ConflictsDefaults;
import me.whereareiam.identica.config.ConfigProvider;
import me.whereareiam.identica.model.config.provider.Conflicts;

import java.nio.file.Path;

@Singleton
public class ConflictsProvider extends ConfigProvider<Conflicts> {
	@Inject
	public ConflictsProvider(
			@Named("providersPath") Path providersPath,
			Registry<Reloadable> registry
	) {
		super(providersPath, "conflicts", Conflicts.class, registry, configure(ConflictsDefaults.class, Conflicts.class));
	}
}
