package me.whereareiam.identica.common.config.provider;

import com.google.inject.Provider;
import me.whereareiam.identica.Reloadable;
import me.whereareiam.identica.config.ConfigProvider;
import me.whereareiam.identica.registry.Registry;

import java.nio.file.Path;

/**
 * Adapter-layer base that wires ConfigProvider into our reload registry
 * and exposes the resolved base path for subclasses.
 */
public abstract class DefaultConfigProvider<T> extends ConfigProvider<T> implements Provider<T> {
	private final Path basePath;

	protected DefaultConfigProvider(Path basePath, Registry<Reloadable> reloadables) {
		this.basePath = basePath;
		reloadables.register(this);
	}

	protected Path getBasePath() {
		return basePath;
	}
}
