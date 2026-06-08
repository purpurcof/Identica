package me.whereareiam.identica.common.config.defaults;

import com.google.inject.Singleton;
import me.whereareiam.configura.merge.defaults.DefaultsProvider;
import me.whereareiam.identica.model.config.persistence.SqlitePersistence;
import org.jetbrains.annotations.NotNull;

@Singleton
public class PersistenceDefaults implements DefaultsProvider<SqlitePersistence> {
	@Override
	public SqlitePersistence supply(@NotNull SqlitePersistence config) {
		return config;
	}
}
