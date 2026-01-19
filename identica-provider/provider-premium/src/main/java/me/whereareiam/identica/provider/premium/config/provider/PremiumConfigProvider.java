package me.whereareiam.identica.provider.premium.config.provider;

import com.google.inject.Provider;
import me.whereareiam.identica.Reloadable;
import me.whereareiam.identica.config.ConfigProvider;
import me.whereareiam.identica.registry.Registry;

import java.nio.file.Path;

public abstract class PremiumConfigProvider<T> extends ConfigProvider<T> implements Provider<T> {
	private final Path basePath;

	protected PremiumConfigProvider(Path basePath, Registry<Reloadable> reloadables) {
		this.basePath = basePath;
		reloadables.register(this);
	}

	protected Path getBasePath() {
		return basePath;
	}
}
