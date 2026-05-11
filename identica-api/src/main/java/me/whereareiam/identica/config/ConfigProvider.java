package me.whereareiam.identica.config;

import com.google.inject.Provider;
import me.whereareiam.configura.Config;
import me.whereareiam.configura.merge.MergeDefaultsProvider;
import me.whereareiam.configura.migration.MigrationDefinition;
import me.whereareiam.identica.Reloadable;
import me.whereareiam.identica.Registry;

import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * Reusable runner for configuration providers.
 */
public abstract class ConfigProvider<T> implements Provider<T>, Reloadable {
	private final Path path;
	private final Class<? extends T> type;
	private final Config config;
	private T value;

	protected ConfigProvider(
			Path basePath,
			String fileName,
			Class<? extends T> type,
			Registry<Reloadable> reloadables,
			Config config
	) {
		this.path = basePath.resolve(fileName);
		this.type = type;
		this.config = config;
		reloadables.register(this);
	}

	@Override
	public T get() {
		if (value != null) return value;

		value = load();
		return value;
	}

	@Override
	public void reload() {
		value = load();
	}

	protected final Path getPath() {
		return path;
	}

	protected T load() {
		return config.update(path, resolveType(path));
	}

	protected Class<? extends T> resolveType(Path path) {
		return type;
	}

	protected final <R> R read(Path path, Class<R> type) {
		return config.read(path, type);
	}

	protected static Config configure(
			Class<? extends MergeDefaultsProvider<?>> providerClass,
			Class<?>... versionedTypes
	) {
		Versioned<?>[] declarations = new Versioned<?>[versionedTypes.length];
		for (int i = 0; i < versionedTypes.length; i++) {
			declarations[i] = versioned(versionedTypes[i]);
		}

		return configure(Config.defaults(), providerClass, declarations);
	}

	protected static Config configure(
			Config baseConfig,
			Class<? extends MergeDefaultsProvider<?>> providerClass,
			Class<?>... versionedTypes
	) {
		Versioned<?>[] declarations = new Versioned<?>[versionedTypes.length];
		for (int i = 0; i < versionedTypes.length; i++) {
			declarations[i] = versioned(versionedTypes[i]);
		}

		return configure(baseConfig, providerClass, declarations);
	}

	protected static Config configure(
			Class<? extends MergeDefaultsProvider<?>> providerClass,
			Versioned<?>... versionedTypes
	) {
		return configure(Config.defaults(), providerClass, versionedTypes);
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	protected static Config configure(
			Config baseConfig,
			Class<? extends MergeDefaultsProvider<?>> providerClass,
			Versioned<?>... versionedTypes
	) {
		Config config = baseConfig.withDefaults((Class) providerClass);
		if (versionedTypes == null) return config;

		for (Versioned<?> versionedType : versionedTypes) {
			config = registerVersioned(config, versionedType);
		}

		return config;
	}

	protected static <T> Versioned<T> versioned(Class<T> type) {
		return versioned(type, spec -> spec
				.currentVersion(1)
				.assumeVersionWhenMissing(1));
	}

	protected static <T> Versioned<T> versioned(Class<T> type, Consumer<MigrationDefinition<T>> customizer) {
		return new Versioned<>(type, customizer);
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static Config registerVersioned(Config config, Versioned<?> versioned) {
		if (versioned == null) return config;
		Class<?> type = versioned.type();
		if (type == null || config.isVersioned(type)) return config;
		return config.withVersioned((Class) type, (Consumer) versioned.customizer());
	}

	protected record Versioned<T>(Class<T> type, Consumer<MigrationDefinition<T>> customizer) {
	}
}
