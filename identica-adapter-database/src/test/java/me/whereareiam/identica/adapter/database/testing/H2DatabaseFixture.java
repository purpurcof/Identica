package me.whereareiam.identica.adapter.database.testing;

import me.whereareiam.identica.model.config.persistence.H2Persistence;
import me.whereareiam.identica.model.config.persistence.Persistence;

import java.nio.file.Path;

public final class H2DatabaseFixture extends AbstractDatabaseFixture {
	@Override
	public String name() {
		return "h2";
	}

	@Override
	protected Persistence createPersistence(Path dataPath) {
		H2Persistence persistence = new H2Persistence();
		persistence.setFile("mem:identica_test");
		persistence.setOptions("DB_CLOSE_DELAY=-1");
		return persistence;
	}
}
