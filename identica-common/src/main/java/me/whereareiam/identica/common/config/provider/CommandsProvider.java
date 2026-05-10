package me.whereareiam.identica.common.config.provider;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import me.whereareiam.identica.Reloadable;
import me.whereareiam.identica.common.config.template.CommandsTemplate;
import me.whereareiam.identica.config.ConfigProvider;
import me.whereareiam.identica.model.config.Commands;
import me.whereareiam.identica.Registry;

import java.nio.file.Path;

@Singleton
public class CommandsProvider extends ConfigProvider<Commands> {
	@Inject
	public CommandsProvider(
			@Named("dataPath") Path dataPath,
			Registry<Reloadable> registry
	) {
		super(dataPath, "commands", Commands.class, registry, configure(CommandsTemplate.class, Commands.class));
	}
}
