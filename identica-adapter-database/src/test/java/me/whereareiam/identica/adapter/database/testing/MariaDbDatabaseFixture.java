package me.whereareiam.identica.adapter.database.testing;

import me.whereareiam.identica.model.config.persistence.Persistence;
import me.whereareiam.identica.model.config.persistence.external.MysqlPersistence;
import org.testcontainers.mariadb.MariaDBContainer;

import java.nio.file.Path;

public final class MariaDbDatabaseFixture extends AbstractDatabaseFixture {
	private final MariaDBContainer container;

	public MariaDbDatabaseFixture(MariaDBContainer container) {
		this.container = container;
	}

	@Override
	public String name() {
		return "mariadb";
	}

	@Override
	protected Persistence createPersistence(Path dataPath) {
		if (container == null || !container.isRunning()) {
			throw new IllegalStateException("MariaDB container is not running");
		}

		MysqlPersistence persistence = new MysqlPersistence();
		persistence.setHost(container.getHost());
		persistence.setPort(container.getMappedPort(3306));
		persistence.setDatabase(container.getDatabaseName());
		persistence.setUsername(container.getUsername());
		persistence.setPassword(container.getPassword());
		return persistence;
	}
}
