package me.whereareiam.identica.adapter.database;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import me.whereareiam.identica.adapter.database.account.DefaultAccountPersistenceService;
import me.whereareiam.identica.adapter.database.account.DefaultAccountReservationPersistenceService;
import me.whereareiam.identica.adapter.database.provider.DefaultProviderLinkPersistenceService;
import me.whereareiam.identica.adapter.database.provider.DefaultProviderProfilePersistenceService;
import me.whereareiam.identica.adapter.database.provider.JdbiProvider;
import me.whereareiam.identica.adapter.database.repository.account.AccountRepository;
import me.whereareiam.identica.adapter.database.repository.account.AccountReservationRepository;
import me.whereareiam.identica.adapter.database.repository.provider.ProviderLinkRepository;
import me.whereareiam.identica.adapter.database.repository.provider.ProviderProfileRepository;
import me.whereareiam.identica.database.AccountPersistenceService;
import me.whereareiam.identica.database.AccountReservationPersistenceService;
import me.whereareiam.identica.database.DatabaseService;
import me.whereareiam.identica.database.provider.ProviderLinkPersistenceService;
import me.whereareiam.identica.database.provider.ProviderProfilePersistenceService;
import me.whereareiam.identica.database.schema.SchemaBootstrap;
import org.jdbi.v3.core.Jdbi;

public class DatabaseConfiguration extends AbstractModule {
	@Override
	protected void configure() {
		bind(Jdbi.class).toProvider(JdbiProvider.class);
		bind(DatabaseService.class).to(DefaultDatabaseService.class).asEagerSingleton();
		bind(SchemaBootstrap.class).to(DefaultDatabaseService.class);
		bind(AccountPersistenceService.class).to(DefaultAccountPersistenceService.class).asEagerSingleton();
		bind(AccountReservationPersistenceService.class).to(DefaultAccountReservationPersistenceService.class).asEagerSingleton();
		bind(ProviderLinkPersistenceService.class).to(DefaultProviderLinkPersistenceService.class).asEagerSingleton();
		bind(ProviderProfilePersistenceService.class).to(DefaultProviderProfilePersistenceService.class).asEagerSingleton();
	}

	@Provides
	@Singleton
	public AccountRepository provideAccountRepository(Jdbi jdbi) {
		return jdbi.onDemand(AccountRepository.class);
	}

	@Provides
	@Singleton
	public AccountReservationRepository provideAccountReservationRepository(Jdbi jdbi) {
		return jdbi.onDemand(AccountReservationRepository.class);
	}

	@Provides
	@Singleton
	public ProviderLinkRepository provideProviderLinkRepository(Jdbi jdbi) {
		return jdbi.onDemand(ProviderLinkRepository.class);
	}

	@Provides
	@Singleton
	public ProviderProfileRepository provideProviderProfileRepository(Jdbi jdbi) {
		return jdbi.onDemand(ProviderProfileRepository.class);
	}

}
