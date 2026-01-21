package me.whereareiam.identica.provider.premium.config.provider;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import me.whereareiam.configura.Config;
import me.whereareiam.identica.Reloadable;
import me.whereareiam.identica.provider.premium.config.PremiumMessages;
import me.whereareiam.identica.provider.premium.config.template.PremiumMessagesTemplate;
import me.whereareiam.identica.registry.Registry;

import java.nio.file.Path;

@Singleton
public class PremiumMessagesProvider extends PremiumConfigProvider<PremiumMessages> {
	@Inject
	public PremiumMessagesProvider(
			@Named("workingPath") Path workingPath,
			Registry<Reloadable> reloadables
	) {
		super(workingPath, reloadables);
	}

	@Override
	protected PremiumMessages load() {
		return Config.update(getBasePath().resolve("messages"), PremiumMessages.class);
	}

	@Override
	protected void registerTemplate() {
		Config.registerTemplate(PremiumMessagesTemplate.class);
	}
}
