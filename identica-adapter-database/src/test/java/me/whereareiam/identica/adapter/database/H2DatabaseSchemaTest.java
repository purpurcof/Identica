package me.whereareiam.identica.adapter.database;

import me.whereareiam.identica.model.config.persistence.H2Persistence;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

class H2DatabaseSchemaTest {
	@Test
	void initializesSchemaInH2(@TempDir Path tempDir) {
		H2Persistence persistence = new H2Persistence();
		persistence.setFile("identica-test");

		DatabaseSchemaTestSupport.runSchemaTest(persistence, tempDir);
	}
}
