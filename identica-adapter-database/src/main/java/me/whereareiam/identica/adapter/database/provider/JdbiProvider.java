package me.whereareiam.identica.adapter.database.provider;

import com.google.inject.Inject;
import com.google.inject.Provider;
import me.whereareiam.identica.adapter.database.DefaultDatabaseService;
import org.jdbi.v3.core.Jdbi;

public class JdbiProvider implements Provider<Jdbi> {
	private final DefaultDatabaseService databaseService;

	@Inject
	public JdbiProvider(DefaultDatabaseService databaseService) {
		this.databaseService = databaseService;
	}

	@Override
	public Jdbi get() {
		if (!databaseService.isInitialized()) {
			throw new IllegalStateException("Database is not initialized yet");
		}

		return databaseService.getJdbi();
	}
}
