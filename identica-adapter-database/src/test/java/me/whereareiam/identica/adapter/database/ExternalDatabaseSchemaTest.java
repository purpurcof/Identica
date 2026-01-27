package me.whereareiam.identica.adapter.database;

import me.whereareiam.identica.model.config.persistence.external.MysqlPersistence;
import me.whereareiam.identica.model.config.persistence.external.PostgresPersistence;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.file.Files;
import java.nio.file.Path;

@Testcontainers(disabledWithoutDocker = true)
class ExternalDatabaseSchemaTest {
	@Container
	private static final PostgreSQLContainer<?> POSTGRES =
			new PostgreSQLContainer<>("postgres:18.1");

	@Container
	private static final MariaDBContainer<?> MARIADB =
			new MariaDBContainer<>("mariadb:12");

	@Test
	void initializesSchemaInPostgres() throws Exception {
		PostgresPersistence persistence = new PostgresPersistence();
		persistence.setHost(POSTGRES.getHost());
		persistence.setPort(POSTGRES.getFirstMappedPort());
		persistence.setDatabase(POSTGRES.getDatabaseName());
		persistence.setUsername(POSTGRES.getUsername());
		persistence.setPassword(POSTGRES.getPassword());

		Path dataPath = Files.createTempDirectory("identica-postgres-test");
		DatabaseSchemaTestSupport.runSchemaTest(persistence, dataPath);
	}

	@Test
	void initializesSchemaInMariaDb() throws Exception {
		MysqlPersistence persistence = new MysqlPersistence();
		persistence.setHost(MARIADB.getHost());
		persistence.setPort(MARIADB.getFirstMappedPort());
		persistence.setDatabase(MARIADB.getDatabaseName());
		persistence.setUsername(MARIADB.getUsername());
		persistence.setPassword(MARIADB.getPassword());

		Path dataPath = Files.createTempDirectory("identica-mariadb-test");
		DatabaseSchemaTestSupport.runSchemaTest(persistence, dataPath);
	}
}
