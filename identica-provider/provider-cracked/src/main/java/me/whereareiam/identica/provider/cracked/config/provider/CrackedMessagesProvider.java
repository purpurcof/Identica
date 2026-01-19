package me.whereareiam.identica.provider.cracked.config.provider;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import me.whereareiam.configura.Config;
import me.whereareiam.identica.Reloadable;
import me.whereareiam.identica.provider.cracked.config.CrackedMessages;
import me.whereareiam.identica.provider.cracked.config.template.CrackedMessagesTemplate;
import me.whereareiam.identica.registry.Registry;

import java.nio.file.Path;

@Singleton
public class CrackedMessagesProvider extends CrackedConfigProvider<CrackedMessages> {
	@Inject
	public CrackedMessagesProvider(@Named("providersPath") Path providersPath, Registry<Reloadable> reloadables) {
		super(providersPath.resolve("Cracked"), reloadables);
	}

	@Override
	protected CrackedMessages load() {
		return Config.update(getBasePath().resolve("messages"), CrackedMessages.class);
	}

	@Override
	protected void registerTemplate() {
		Config.registerTemplate(CrackedMessagesTemplate.class);
	}
}
