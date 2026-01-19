package me.whereareiam.identica.common.config.provider;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import me.whereareiam.configura.Config;
import me.whereareiam.identica.Reloadable;
import me.whereareiam.identica.common.config.template.MessagesTemplate;
import me.whereareiam.identica.model.config.Messages;
import me.whereareiam.identica.registry.Registry;

import java.nio.file.Path;

@Singleton
public class MessagesProvider extends DefaultConfigProvider<Messages> {
	@Inject
	public MessagesProvider(@Named("dataPath") Path dataPath, Registry<Reloadable> registry) {
		super(dataPath, registry);
	}

	@Override
	protected Messages load() {
		return Config.update(getBasePath().resolve("messages"), Messages.class);
	}

	@Override
	protected void registerTemplate() {
		Config.registerTemplate(MessagesTemplate.class);
	}
}
