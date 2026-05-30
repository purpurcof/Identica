package me.whereareiam.identica.common.config.provider;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import me.whereareiam.configura.Config;
import me.whereareiam.configura.Configura;
import me.whereareiam.identica.Registry;
import me.whereareiam.identica.Reloadable;
import me.whereareiam.identica.common.config.defaults.VerificationDefaults;
import me.whereareiam.identica.config.ConfigProvider;
import me.whereareiam.identica.model.config.Verification;

import java.nio.file.Path;

@Singleton
public class VerificationProvider extends ConfigProvider<Verification> {
	@Inject
	public VerificationProvider(
			@Named("dataPath") Path dataPath,
			Registry<Reloadable> registry
	) {
		super(dataPath, "verification", Verification.class, registry);
	}

	@Override
	protected Configura configura() {
		return versioned(Config.configured().withDefaults(VerificationDefaults.class), Verification.class);
	}
}
