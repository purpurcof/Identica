package me.whereareiam.identica.provider.credential.database;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import me.whereareiam.identica.database.schema.SchemaContributor;
import me.whereareiam.identica.provider.credential.database.repository.CredentialAccountHistoryRepository;
import me.whereareiam.identica.provider.credential.database.repository.CredentialAccountRepository;
import org.jdbi.v3.core.Jdbi;

public class DatabaseModule extends AbstractModule {
	@Override
	protected void configure() {
		bind(CredentialAccountPersistenceService.class).to(DefaultCredentialAccountPersistenceService.class).asEagerSingleton();
		Multibinder.newSetBinder(binder(), SchemaContributor.class)
				.addBinding()
				.to(CredentialSchemaContributor.class);
	}

	@Provides
	@Singleton
	public CredentialAccountRepository provideCredentialAccountRepository(Jdbi jdbi) {
		return jdbi.onDemand(CredentialAccountRepository.class);
	}

	@Provides
	@Singleton
	public CredentialAccountHistoryRepository provideCredentialAccountHistoryRepository(Jdbi jdbi) {
		return jdbi.onDemand(CredentialAccountHistoryRepository.class);
	}
}
