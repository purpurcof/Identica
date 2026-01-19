package me.whereareiam.identica.adapter.database;

import com.google.inject.AbstractModule;
import me.whereareiam.identica.adapter.database.provider.JdbiProvider;
import me.whereareiam.identica.database.DatabaseService;
import org.jdbi.v3.core.Jdbi;

public class DatabaseConfiguration extends AbstractModule {
	@Override
	protected void configure() {
		bind(Jdbi.class).toProvider(JdbiProvider.class);
		bind(DatabaseService.class).to(DefaultDatabaseService.class).asEagerSingleton();
	}
}
