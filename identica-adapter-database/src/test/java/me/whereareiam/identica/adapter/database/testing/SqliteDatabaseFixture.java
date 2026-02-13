package me.whereareiam.identica.adapter.database.testing;

import me.whereareiam.identica.model.config.persistence.Persistence;
import me.whereareiam.identica.model.config.persistence.SqlitePersistence;
import org.jdbi.v3.core.Handle;

import java.nio.file.Path;

public final class SqliteDatabaseFixture extends AbstractDatabaseFixture {
	@Override
	public String name() {
		return "sqlite";
	}

	@Override
	protected Persistence createPersistence(Path dataPath) {
		SqlitePersistence persistence = new SqlitePersistence();
		persistence.setFile("identica-test.db");
		return persistence;
	}

	@Override
	public void prepareHandle(Handle handle) {
		handle.execute("PRAGMA foreign_keys = ON");
	}
}
