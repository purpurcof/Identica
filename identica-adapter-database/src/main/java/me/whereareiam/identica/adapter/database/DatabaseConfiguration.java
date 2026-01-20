package me.whereareiam.identica.adapter.database;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import me.whereareiam.identica.adapter.database.account.DefaultAccountPersistenceService;
import me.whereareiam.identica.adapter.database.identity.DefaultIdentityPersistenceService;
import me.whereareiam.identica.adapter.database.provider.JdbiProvider;
import me.whereareiam.identica.adapter.database.repository.account.AccountRepository;
import me.whereareiam.identica.adapter.database.repository.identity.IdentityRepository;
import me.whereareiam.identica.database.AccountPersistenceService;
import me.whereareiam.identica.database.DatabaseService;
import me.whereareiam.identica.database.IdentityPersistenceService;
import org.jdbi.v3.core.Jdbi;

public class DatabaseConfiguration extends AbstractModule {
	@Override
	protected void configure() {
		bind(Jdbi.class).toProvider(JdbiProvider.class);
		bind(DatabaseService.class).to(DefaultDatabaseService.class).asEagerSingleton();
		bind(AccountPersistenceService.class).to(DefaultAccountPersistenceService.class).asEagerSingleton();
		bind(IdentityPersistenceService.class).to(DefaultIdentityPersistenceService.class).asEagerSingleton();
	}

	@Provides
	@Singleton
	public AccountRepository provideAccountRepository(Jdbi jdbi) {
		return jdbi.onDemand(AccountRepository.class);
	}

	@Provides
	@Singleton
	public IdentityRepository provideIdentityRepository(Jdbi jdbi) {
		return jdbi.onDemand(IdentityRepository.class);
	}
}
