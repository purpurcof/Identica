package me.whereareiam.identica.common.config.provider;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import me.whereareiam.configura.Config;
import me.whereareiam.identica.Reloadable;
import me.whereareiam.identica.common.config.template.PersistenceTemplate;
import me.whereareiam.identica.model.config.persistence.Persistence;
import me.whereareiam.identica.model.config.persistence.SqlitePersistence;
import me.whereareiam.identica.Registry;

import java.nio.file.Path;

@Singleton
public class PersistenceProvider extends DefaultConfigProvider<Persistence> {
	@Inject
	public PersistenceProvider(@Named("dataPath") Path dataPath, Registry<Reloadable> registry) {
		super(dataPath, registry);
	}

	@Override
	protected Persistence load() {
		Path path = getBasePath().resolve("persistence");
		return Config.update(path, resolvePersistenceClass(path));
	}

	@Override
	protected void registerTemplate() {
		Config.registerTemplate(PersistenceTemplate.class);
	}

	private Class<? extends Persistence> resolvePersistenceClass(Path path) {
		try {
			return Config.load(path, Persistence.class).getClass().asSubclass(Persistence.class);
		} catch (RuntimeException ignored) {
			return SqlitePersistence.class;
		}
	}
}
