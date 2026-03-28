package me.whereareiam.identica.provider.cracked.config.provider;

import com.google.inject.Provider;
import me.whereareiam.identica.Reloadable;
import me.whereareiam.identica.config.ConfigProvider;
import me.whereareiam.identica.Registry;

import java.nio.file.Path;

public abstract class CrackedConfigProvider<T> extends ConfigProvider<T> implements Provider<T> {
	private final Path basePath;

	protected CrackedConfigProvider(Path basePath, Registry<Reloadable> reloadables) {
		this.basePath = basePath;
		reloadables.register(this);
	}

	protected Path getBasePath() {
		return basePath;
	}
}
