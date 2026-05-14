package me.whereareiam.identica.provider.credential.database;

import me.whereareiam.identica.adapter.database.DefaultDatabaseService;
import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.model.config.persistence.H2Persistence;
import me.whereareiam.identica.model.config.persistence.SqlitePersistence;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Credential Dialectica Schema Contributor")
class CredentialSchemaContributorTest {
	private DefaultDatabaseService databaseService;
	private Jdbi jdbi;

    @BeforeEach
	void setUp() throws Exception {
        Path dataPath = Files.createTempDirectory("credential-schema-contributor-");

		H2Persistence persistence = new H2Persistence();
		persistence.setFile("mem:credential_migrations;DB_CLOSE_DELAY=-1");
		persistence.setOptions("MODE=PostgreSQL");

		databaseService = new DefaultDatabaseService(
				persistence,
				Mockito.mock(EventManager.class),
                dataPath
		);
		jdbi = databaseService.getJdbi();

		jdbi.useHandle(handle -> {
			handle.execute("DROP TABLE IF EXISTS dialectica_schema_migrations");
			handle.execute("DROP TABLE IF EXISTS identica_provider_credential_accounts_history");
			handle.execute("DROP TABLE IF EXISTS identica_provider_credential_accounts");
			handle.execute("DROP TABLE IF EXISTS identica_cracked_account_passwords");
			handle.execute("DROP TABLE IF EXISTS identica_cracked_accounts");
		});
	}

	@AfterEach
	void tearDown() {
		if (databaseService != null)
			databaseService.onShutdown(null);
	}

	@Test
	void renamesLegacySingularPasswordHistoryTableToCanonicalName() {
		jdbi.useHandle(handle -> {
			handle.execute("""
					CREATE TABLE identica_cracked_accounts (
						provider_id VARCHAR(64) NOT NULL,
						provider_subject VARCHAR(128) NOT NULL,
						password_hash VARCHAR(512) NOT NULL,
						hashing_method VARCHAR(64) NOT NULL,
						created_at BIGINT,
						updated_at BIGINT,
						PRIMARY KEY (provider_id, provider_subject)
					)
					""");
			handle.execute("""
					CREATE TABLE identica_cracked_account_passwords (
						provider_id VARCHAR(64) NOT NULL,
						provider_subject VARCHAR(128) NOT NULL,
						hashing_method VARCHAR(64) NOT NULL,
						change_reason VARCHAR(32) NOT NULL,
						changed_at BIGINT,
						PRIMARY KEY (provider_id, provider_subject, changed_at)
					)
					""");
		});

		databaseService.apply(new CredentialSchemaContributor());

		assertFalse(tableExists("identica_cracked_accounts"));
		assertFalse(tableExists("identica_cracked_account_passwords"));
		assertTrue(tableExists("identica_provider_credential_accounts"));
		assertTrue(tableExists("identica_provider_credential_accounts_history"));
		assertEquals(2L, migrationRows());
	}

	@Test
	void freshInstallCreatesCanonicalTables() {
		assertFalse(tableExists("dialectica_schema_migrations"));

		databaseService.apply(new CredentialSchemaContributor());

		assertTrue(tableExists("identica_provider_credential_accounts"));
		assertTrue(tableExists("identica_provider_credential_accounts_history"));
		assertTrue(tableExists("dialectica_schema_migrations"));
	}

	@Test
	void mixedStateFailsDeterministically() {
		jdbi.useHandle(handle -> {
			handle.execute("""
					CREATE TABLE identica_cracked_accounts (
						provider_id VARCHAR(64) NOT NULL,
						provider_subject VARCHAR(128) NOT NULL,
						password_hash VARCHAR(512) NOT NULL,
						hashing_method VARCHAR(64) NOT NULL,
						created_at BIGINT,
						updated_at BIGINT,
						PRIMARY KEY (provider_id, provider_subject)
					)
					""");
			handle.execute("""
					CREATE TABLE identica_provider_credential_accounts (
						provider_id VARCHAR(64) NOT NULL,
						provider_subject VARCHAR(128) NOT NULL,
						password_hash VARCHAR(512) NOT NULL,
						hashing_method VARCHAR(64) NOT NULL,
						created_at BIGINT,
						updated_at BIGINT,
						PRIMARY KEY (provider_id, provider_subject)
					)
					""");
		});

		IllegalStateException exception = assertThrows(
				IllegalStateException.class,
				() -> databaseService.apply(new CredentialSchemaContributor())
		);
		assertTrue(exception.getMessage().contains("Failed to apply migration credential-accounts:1"));
		assertNotNull(exception.getCause());
		assertTrue(exception.getCause().getMessage().contains("Cannot rename identica_cracked_accounts"));
		assertEquals(0L, migrationRows());
	}

	@Test
	void sqliteRuntimeStyleStartupRenamesLegacyTablesBeforeCreatingCanonicalOnes() throws Exception {
		if (databaseService != null)
			databaseService.onShutdown(null);

		Path sqlitePath = Files.createTempDirectory("credential-schema-contributor-sqlite-");
		SqlitePersistence persistence = new SqlitePersistence();
		persistence.setFile("identica-test.db");

		databaseService = new DefaultDatabaseService(
				persistence,
				Mockito.mock(EventManager.class),
				sqlitePath
		);
		jdbi = databaseService.getJdbi();

		jdbi.useHandle(handle -> {
			handle.execute("PRAGMA foreign_keys = ON");
			handle.execute("""
					CREATE TABLE identica_cracked_accounts (
						provider_id VARCHAR(64) NOT NULL,
						provider_subject VARCHAR(128) NOT NULL,
						password_hash VARCHAR(512) NOT NULL,
						hashing_method VARCHAR(64) NOT NULL,
						created_at BIGINT,
						updated_at BIGINT,
						PRIMARY KEY (provider_id, provider_subject)
					)
					""");
			handle.execute("""
					CREATE TABLE identica_cracked_account_passwords (
						provider_id VARCHAR(64) NOT NULL,
						provider_subject VARCHAR(128) NOT NULL,
						hashing_method VARCHAR(64) NOT NULL,
						change_reason VARCHAR(32) NOT NULL,
						changed_at BIGINT,
						PRIMARY KEY (provider_id, provider_subject, changed_at),
						FOREIGN KEY (provider_id, provider_subject)
							REFERENCES identica_cracked_accounts(provider_id, provider_subject)
							ON DELETE CASCADE
					)
					""");
		});

		databaseService.apply(new CredentialSchemaContributor());

		assertFalse(tableExists("identica_cracked_accounts"));
		assertFalse(tableExists("identica_cracked_account_passwords"));
		assertTrue(tableExists("identica_provider_credential_accounts"));
		assertTrue(tableExists("identica_provider_credential_accounts_history"));
		assertTrue(tableExists("dialectica_schema_migrations"));
		assertEquals(2L, migrationRows());
	}

	private boolean tableExists(String tableName) {
		return jdbi.withHandle(handle -> {
			try {
				try (var resultSet = handle.getConnection().getMetaData().getTables(null, null, tableName, new String[]{"TABLE"})) {
					if (resultSet.next())
						return true;
				}
				try (var resultSet = handle.getConnection().getMetaData().getTables(null, null, tableName.toUpperCase(), new String[]{"TABLE"})) {
					return resultSet.next();
				}
			} catch (Exception exception) {
				throw new IllegalStateException("Failed to inspect table " + tableName, exception);
			}
		});
	}

	private long migrationRows() {
		if (!tableExists("dialectica_schema_migrations"))
			return 0L;
		return jdbi.withHandle(handle -> handle.createQuery("SELECT COUNT(*) FROM dialectica_schema_migrations")
				.mapTo(Long.class)
				.one());
	}
}
