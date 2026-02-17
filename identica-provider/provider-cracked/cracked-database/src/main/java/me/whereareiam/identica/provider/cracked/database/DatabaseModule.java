package me.whereareiam.identica.provider.cracked.database;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import me.whereareiam.identica.provider.cracked.database.repository.CrackedAccountPasswordRepository;
import me.whereareiam.identica.provider.cracked.database.repository.CrackedAccountRepository;
import org.jdbi.v3.core.Jdbi;

public class DatabaseModule extends AbstractModule {
	@Override
	protected void configure() {
		bind(CrackedAccountPersistenceService.class).to(DefaultCrackedAccountPersistenceService.class).asEagerSingleton();
		bind(DatabaseInitializer.class).asEagerSingleton();
	}

	@Provides
	@Singleton
	public CrackedAccountRepository provideCrackedAccountRepository(Jdbi jdbi) {
		return jdbi.onDemand(CrackedAccountRepository.class);
	}

	@Provides
	@Singleton
	public CrackedAccountPasswordRepository provideCrackedAccountPasswordRepository(Jdbi jdbi) {
		return jdbi.onDemand(CrackedAccountPasswordRepository.class);
	}
}
