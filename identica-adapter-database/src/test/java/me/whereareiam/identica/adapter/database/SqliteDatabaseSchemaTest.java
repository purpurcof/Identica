package me.whereareiam.identica.adapter.database;

import me.whereareiam.identica.model.config.persistence.SqlitePersistence;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

class SqliteDatabaseSchemaTest {
	@Test
	void initializesSchemaInSqlite(@TempDir Path tempDir) {
		SqlitePersistence persistence = new SqlitePersistence();
		persistence.setFile("identica-test.db");

		DatabaseSchemaTestSupport.runSchemaTest(persistence, tempDir);
	}
}
