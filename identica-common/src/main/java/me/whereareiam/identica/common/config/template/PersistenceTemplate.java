package me.whereareiam.identica.common.config.template;

import com.google.inject.Singleton;
import me.whereareiam.configura.TemplateProvider;
import me.whereareiam.identica.model.config.persistence.SqlitePersistence;

@Singleton
public class PersistenceTemplate implements TemplateProvider<SqlitePersistence> {
	@Override
	public SqlitePersistence supply(SqlitePersistence config) {
		return config;
	}
}
