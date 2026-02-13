package me.whereareiam.identica.adapter.database.connection;

import me.whereareiam.identica.model.config.persistence.H2Persistence;
import me.whereareiam.identica.model.config.persistence.SqlitePersistence;
import me.whereareiam.identica.model.config.persistence.external.MysqlPersistence;
import me.whereareiam.identica.model.config.persistence.external.PostgresPersistence;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JdbcUrlFactoryTest {
	@Test
	void sqliteDefaultsToIdenticaDbWhenBlank() {
		Path dataPath = Path.of("build", "test-data");
		SqlitePersistence persistence = new SqlitePersistence();
		persistence.setFile(" ");

		String url = JdbcUrlFactory.create(persistence, dataPath);

		assertEquals("jdbc:sqlite:" + dataPath.resolve("identica.db"), url);
	}

	@Test
	void sqliteUsesCustomFileName() {
		Path dataPath = Path.of("build", "test-data");
		SqlitePersistence persistence = new SqlitePersistence();
		persistence.setFile("custom.db");

		String url = JdbcUrlFactory.create(persistence, dataPath);

		assertEquals("jdbc:sqlite:" + dataPath.resolve("custom.db"), url);
	}

	@Test
	void h2UsesMemTargetWhenProvided() {
		Path dataPath = Path.of("build", "test-data");
		H2Persistence persistence = new H2Persistence();
		persistence.setFile("mem:identica_test");
		persistence.setOptions("");

		String url = JdbcUrlFactory.create(persistence, dataPath);

		assertEquals("jdbc:h2:mem:identica_test", url);
	}

	@Test
	void h2UsesFileTargetWithOptions() {
		Path dataPath = Path.of("build", "test-data");
		H2Persistence persistence = new H2Persistence();
		persistence.setFile("identica");
		persistence.setOptions("MODE=PostgreSQL");

		String url = JdbcUrlFactory.create(persistence, dataPath);

		assertEquals("jdbc:h2:file:" + dataPath.resolve("identica") + ";MODE=PostgreSQL", url);
	}

	@Test
	void postgresExternalUrlIsFormatted() {
		Path dataPath = Path.of("build", "test-data");
		PostgresPersistence persistence = new PostgresPersistence();
		persistence.setHost("db.local");
		persistence.setPort(5433);
		persistence.setDatabase("identica");

		String url = JdbcUrlFactory.create(persistence, dataPath);

		assertEquals("jdbc:postgresql://db.local:5433/identica", url);
	}

	@Test
	void mysqlExternalUrlIsFormatted() {
		Path dataPath = Path.of("build", "test-data");
		MysqlPersistence persistence = new MysqlPersistence();
		persistence.setHost("db.local");
		persistence.setPort(3307);
		persistence.setDatabase("identica");

		String url = JdbcUrlFactory.create(persistence, dataPath);

		assertEquals("jdbc:mariadb://db.local:3307/identica", url);
	}
}
