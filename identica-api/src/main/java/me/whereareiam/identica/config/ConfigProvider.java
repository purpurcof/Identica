package me.whereareiam.identica.config;

import com.google.inject.Provider;
import me.whereareiam.configura.Config;
import me.whereareiam.configura.Configura;
import me.whereareiam.configura.merge.defaults.MergeDefaultsProvider;
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
	private final ConfigurationSpec configuration;
	private T value;

	protected ConfigProvider(
			Path basePath,
			String fileName,
			Class<? extends T> type,
			Registry<Reloadable> reloadables,
			ConfigurationSpec configuration
	) {
		this.path = basePath.resolve(fileName);
		this.type = type;
		this.configuration = configuration;
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
		return configuration.update(path, resolveType(path));
	}

	protected Class<? extends T> resolveType(Path path) {
		return type;
	}

	protected final <R> R read(Path path, Class<R> type) {
		return configuration.read(path, type);
	}

	protected static ConfigurationSpec configure(
			Class<? extends MergeDefaultsProvider<?>> providerClass,
			Class<?>... versionedTypes
	) {
		Versioned<?>[] declarations = new Versioned<?>[versionedTypes.length];
		for (int i = 0; i < versionedTypes.length; i++) {
			declarations[i] = versioned(versionedTypes[i]);
		}

		return configure(ConfigurationSpec.defaults(), providerClass, declarations);
	}

	protected static ConfigurationSpec configure(
			ConfigurationSpec baseConfiguration,
			Class<? extends MergeDefaultsProvider<?>> providerClass,
			Class<?>... versionedTypes
	) {
		Versioned<?>[] declarations = new Versioned<?>[versionedTypes.length];
		for (int i = 0; i < versionedTypes.length; i++) {
			declarations[i] = versioned(versionedTypes[i]);
		}

		return configure(baseConfiguration, providerClass, declarations);
	}

	protected static ConfigurationSpec configure(
			Class<? extends MergeDefaultsProvider<?>> providerClass,
			Versioned<?>... versionedTypes
	) {
		return configure(ConfigurationSpec.defaults(), providerClass, versionedTypes);
	}

	protected static ConfigurationSpec configure(
			ConfigurationSpec baseConfiguration,
			Class<? extends MergeDefaultsProvider<?>> providerClass,
			Versioned<?>... versionedTypes
	) {
		return baseConfiguration.with(providerClass, versionedTypes);
	}

	protected static <T> Versioned<T> versioned(Class<T> type) {
		return versioned(type, spec -> spec
				.currentVersion(1)
				.assumeVersionWhenMissing(1));
	}

	protected static <T> Versioned<T> versioned(Class<T> type, Consumer<MigrationDefinition<T>> customizer) {
		return new Versioned<>(type, customizer);
	}

	protected record Versioned<T>(Class<T> type, Consumer<MigrationDefinition<T>> customizer) {
	}

	protected static final class ConfigurationSpec {
		private final Class<? extends MergeDefaultsProvider<?>> providerClass;
		private final Versioned<?>[] versionedTypes;

		private ConfigurationSpec(
				Class<? extends MergeDefaultsProvider<?>> providerClass,
				Versioned<?>[] versionedTypes
		) {
			this.providerClass = providerClass;
			this.versionedTypes = versionedTypes != null ? versionedTypes.clone() : new Versioned<?>[0];
		}

		public static ConfigurationSpec defaults() {
			return new ConfigurationSpec(null, new Versioned<?>[0]);
		}

		public ConfigurationSpec with(
				Class<? extends MergeDefaultsProvider<?>> providerClass,
				Versioned<?>... versionedTypes
		) {
			return new ConfigurationSpec(providerClass, versionedTypes);
		}

		public <T> T update(Path path, Class<T> type) {
			return configured().update(path, type);
		}

		public <T> T read(Path path, Class<T> type) {
			return configured().read(path, type);
		}

		@SuppressWarnings({"rawtypes", "unchecked"})
		private Configura configured() {
			var config = providerClass != null
					? Config.configured().withDefaults((Class) providerClass)
					: Config.configured();

			for (Versioned<?> versionedType : versionedTypes) {
				if (versionedType == null) continue;

				Class<?> type = versionedType.type();
				if (type == null || config.isVersioned(type)) continue;
				config = config.withVersioned((Class) type, (Consumer) versionedType.customizer());
			}

			return config;
		}
	}
}
