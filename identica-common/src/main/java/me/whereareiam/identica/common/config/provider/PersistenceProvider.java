package me.whereareiam.identica.common.config.provider;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import me.whereareiam.configura.Config;
import me.whereareiam.configura.Configura;
import me.whereareiam.configura.feature.polymorphic.PolymorphicFeature;
import me.whereareiam.identica.Registry;
import me.whereareiam.identica.Reloadable;
import me.whereareiam.identica.common.config.defaults.PersistenceDefaults;
import me.whereareiam.identica.config.ConfigProvider;
import me.whereareiam.identica.model.config.persistence.H2Persistence;
import me.whereareiam.identica.model.config.persistence.Persistence;
import me.whereareiam.identica.model.config.persistence.SqlitePersistence;
import me.whereareiam.identica.model.config.persistence.external.MysqlPersistence;
import me.whereareiam.identica.model.config.persistence.external.PostgresPersistence;

import java.nio.file.Path;

@Singleton
public class PersistenceProvider extends ConfigProvider<Persistence> {
	@Inject
	public PersistenceProvider(
			@Named("dataPath") Path dataPath,
			Registry<Reloadable> registry
	) {
		super(dataPath, "persistence", Persistence.class, registry);
	}

	@Override
	protected Configura configura() {
		Configura configura = Config.configured()
				.withDefaults(PersistenceDefaults.class)
				.withFeature(PolymorphicFeature.defaults());
		configura = versioned(configura, SqlitePersistence.class);
		configura = versioned(configura, H2Persistence.class);
		configura = versioned(configura, MysqlPersistence.class);
		configura = versioned(configura, PostgresPersistence.class);

		return configura;
	}

	@Override
	protected Class<? extends Persistence> resolveType(Path path) {
		return resolvePersistenceClass(path);
	}

	private Class<? extends Persistence> resolvePersistenceClass(Path path) {
		try {
			return read(path, Persistence.class).getClass().asSubclass(Persistence.class);
		} catch (RuntimeException ignored) {
			return SqlitePersistence.class;
		}
	}
}
