package me.whereareiam.identica.adapter.database.testing;

import me.whereareiam.identica.model.config.persistence.Persistence;
import me.whereareiam.identica.model.config.persistence.external.PostgresPersistence;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.nio.file.Path;

public final class PostgresDatabaseFixture extends AbstractDatabaseFixture {
	private final PostgreSQLContainer container;

	public PostgresDatabaseFixture(PostgreSQLContainer container) {
		this.container = container;
	}

	@Override
	public String name() {
		return "postgres";
	}

	@Override
	protected Persistence createPersistence(Path dataPath) {
		if (container == null || !container.isRunning()) {
			throw new IllegalStateException("Postgres container is not running");
		}

		PostgresPersistence persistence = new PostgresPersistence();
		persistence.setHost(container.getHost());
		persistence.setPort(container.getMappedPort(5432));
		persistence.setDatabase(container.getDatabaseName());
		persistence.setUsername(container.getUsername());
		persistence.setPassword(container.getPassword());
		return persistence;
	}
}
