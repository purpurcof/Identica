package me.whereareiam.identica.adapter.database;

import com.zaxxer.hikari.HikariDataSource;
import me.whereareiam.dialectica.Dialectica;
import me.whereareiam.dialectica.SchemaManager;
import me.whereareiam.dialectica.DialectPlugin;
import me.whereareiam.identica.adapter.database.connection.DataSourceFactory;
import me.whereareiam.identica.adapter.database.entity.AccountEntity;
import me.whereareiam.identica.model.config.persistence.Persistence;
import me.whereareiam.identica.type.DatabaseType;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;

import javax.sql.DataSource;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DatabaseSchemaTestSupport {
	static void runSchemaTest(Persistence persistence, Path dataPath) {
		DataSource dataSource = DataSourceFactory.create(persistence, dataPath);
		try {
			Jdbi jdbi = Jdbi.create(dataSource);
			jdbi.installPlugin(new SqlObjectPlugin());
			jdbi.installPlugin(new DialectPlugin(persistence.getType().toString()));

			SchemaManager schemaManager = Dialectica.schema(jdbi)
					.scanPackages(AccountEntity.class.getPackageName())
					.setFailOnError(true);
			schemaManager.initialize();

			assertTableExists(jdbi, persistence.getType(), "identica_accounts");
			assertTableExists(jdbi, persistence.getType(), "identica_provider_links");
			assertTableExists(jdbi, persistence.getType(), "identica_provider_profiles");
			assertTableExists(jdbi, persistence.getType(), "identica_username_history");

			insertRows(jdbi);
			assertRowCounts(jdbi);
		} finally {
			if (dataSource instanceof HikariDataSource hikariDataSource)
				hikariDataSource.close();
		}
	}

	private static void assertTableExists(Jdbi jdbi, DatabaseType type, String tableName) {
		boolean exists = jdbi.withHandle(handle -> switch (type) {
			case POSTGRES -> handle.createQuery(
					"SELECT EXISTS (SELECT 1 FROM information_schema.tables " +
							"WHERE table_schema = 'public' AND table_name = :table)"
			).bind("table", tableName).mapTo(Boolean.class).one();
			case MYSQL -> handle.createQuery(
					"SELECT COUNT(*) FROM information_schema.tables " +
							"WHERE table_schema = DATABASE() AND table_name = :table"
			).bind("table", tableName).mapTo(Long.class).one() > 0;
			case SQLITE -> handle.createQuery(
					"SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name = :table"
			).bind("table", tableName).mapTo(Long.class).one() > 0;
		});

		assertTrue(exists, "Expected table " + tableName + " to exist");
	}

	private static void insertRows(Jdbi jdbi) {
		String accountId = UUID.randomUUID().toString();

		jdbi.useHandle(handle -> {
			handle.createUpdate(
							"INSERT INTO identica_accounts " +
									"(unique_id, username, username_source, created_at, last_seen_at) " +
									"VALUES (:uniqueId, :username, :source, :createdAt, :lastSeenAt)"
					)
					.bind("uniqueId", accountId)
					.bind("username", "Player01")
					.bind("source", "provider")
					.bind("createdAt", 1L)
					.bind("lastSeenAt", 1L)
					.execute();

			handle.createUpdate(
							"INSERT INTO identica_provider_links " +
									"(unique_id, provider_id, provider_subject, is_primary, linked_at, last_seen_at) " +
									"VALUES (:uniqueId, :providerId, :providerSubject, :primary, :linkedAt, :lastSeenAt)"
					)
					.bind("uniqueId", accountId)
					.bind("providerId", "premium")
					.bind("providerSubject", "subject-1")
					.bind("primary", true)
					.bind("linkedAt", 2L)
					.bind("lastSeenAt", 2L)
					.execute();

			handle.createUpdate(
							"INSERT INTO identica_provider_profiles " +
									"(provider_id, provider_subject, provider_username) " +
									"VALUES (:providerId, :providerSubject, :providerUsername)"
					)
					.bind("providerId", "premium")
					.bind("providerSubject", "subject-1")
					.bind("providerUsername", "Player01")
					.execute();

			handle.createUpdate(
							"INSERT INTO identica_username_history " +
									"(unique_id, provider_id, old_username, new_username, source, changed_at) " +
									"VALUES (:uniqueId, :providerId, :oldUsername, :newUsername, :source, :changedAt)"
					)
					.bind("uniqueId", accountId)
					.bind("providerId", "premium")
					.bind("oldUsername", "OldName")
					.bind("newUsername", "Player01")
					.bind("source", "provider")
					.bind("changedAt", 4L)
					.execute();
		});
	}

	private static void assertRowCounts(Jdbi jdbi) {
		long accounts = jdbi.withHandle(handle -> handle
				.createQuery("SELECT COUNT(*) FROM identica_accounts")
				.mapTo(Long.class)
				.one());
		long links = jdbi.withHandle(handle -> handle
				.createQuery("SELECT COUNT(*) FROM identica_provider_links")
				.mapTo(Long.class)
				.one());
		long profiles = jdbi.withHandle(handle -> handle
				.createQuery("SELECT COUNT(*) FROM identica_provider_profiles")
				.mapTo(Long.class)
				.one());
		long history = jdbi.withHandle(handle -> handle
				.createQuery("SELECT COUNT(*) FROM identica_username_history")
				.mapTo(Long.class)
				.one());

		assertEquals(1L, accounts);
		assertEquals(1L, links);
		assertEquals(1L, profiles);
		assertEquals(1L, history);
	}
}
