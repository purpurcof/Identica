package me.whereareiam.identica.common.config.provider;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import me.whereareiam.identica.Reloadable;
import me.whereareiam.identica.common.config.defaults.PersistenceDefaults;
import me.whereareiam.identica.config.ConfigProvider;
import me.whereareiam.identica.model.config.persistence.H2Persistence;
import me.whereareiam.identica.model.config.persistence.Persistence;
import me.whereareiam.identica.model.config.persistence.SqlitePersistence;
import me.whereareiam.identica.model.config.persistence.external.MysqlPersistence;
import me.whereareiam.identica.model.config.persistence.external.PostgresPersistence;
import me.whereareiam.identica.Registry;

import java.nio.file.Path;

@Singleton
public class PersistenceProvider extends ConfigProvider<Persistence> {
	@Inject
	public PersistenceProvider(
			@Named("dataPath") Path dataPath,
			Registry<Reloadable> registry
	) {
		super(
				dataPath,
				"persistence",
				Persistence.class,
				registry,
				configure(
						PersistenceDefaults.class,
						SqlitePersistence.class,
						H2Persistence.class,
						MysqlPersistence.class,
						PostgresPersistence.class
				)
		);
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
