package me.whereareiam.identica.provider.premium.database;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import me.whereareiam.dialectica.Dialectica;
import me.whereareiam.dialectica.SchemaManager;
import me.whereareiam.identica.database.DatabaseService;
import me.whereareiam.identica.logging.Logger;
import me.whereareiam.identica.provider.premium.database.entity.PremiumIdentityEntity;
import me.whereareiam.identica.provider.premium.database.repository.PremiumIdentityRepository;
import org.jdbi.v3.core.Jdbi;

public class PremiumDatabaseConfiguration extends AbstractModule {
	@Override
	protected void configure() {
		requestInjection(this);

		bind(PremiumIdentityPersistenceService.class).asEagerSingleton();
	}

	@Provides
	@Singleton
	public PremiumIdentityRepository providePremiumIdentityRepository(Jdbi jdbi) {
		return jdbi.onDemand(PremiumIdentityRepository.class);
	}

	@Inject
	void initializeSchema(DatabaseService databaseService, Jdbi jdbi) {
		try {
			String entityPackage = PremiumIdentityEntity.class.getPackageName();
			SchemaManager schemaManager = Dialectica.schema(jdbi)
					.scanPackages(entityPackage)
					.setFailOnError(false);

			schemaManager.initialize();
		} catch (Exception e) {
			Logger.severe("Failed to initialize premium schema: %s", e.getMessage());
		}
	}
}
