package me.whereareiam.identica.common.config.provider;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import me.whereareiam.configura.Config;
import me.whereareiam.identica.Reloadable;
import me.whereareiam.identica.Registry;
import me.whereareiam.identica.common.config.template.VerificationTemplate;
import me.whereareiam.identica.model.config.Verification;

import java.nio.file.Path;

@Singleton
public class VerificationProvider extends DefaultConfigProvider<Verification> {
	@Inject
	public VerificationProvider(@Named("dataPath") Path dataPath, Registry<Reloadable> registry) {
		super(dataPath, registry);
	}

	@Override
	protected Verification load() {
		return Config.update(getBasePath().resolve("verification"), Verification.class);
	}

	@Override
	protected void registerTemplate() {
		Config.registerTemplate(VerificationTemplate.class);
	}
}
