package me.whereareiam.identica.common.config.defaults;

import com.google.inject.Singleton;
import me.whereareiam.configura.merge.defaults.MergeDefaultsProvider;
import me.whereareiam.identica.model.config.persistence.SqlitePersistence;
import org.jetbrains.annotations.NotNull;

@Singleton
public class PersistenceDefaults implements MergeDefaultsProvider<SqlitePersistence> {
	@Override
	public SqlitePersistence supply(@NotNull SqlitePersistence config) {
		return config;
	}
}
