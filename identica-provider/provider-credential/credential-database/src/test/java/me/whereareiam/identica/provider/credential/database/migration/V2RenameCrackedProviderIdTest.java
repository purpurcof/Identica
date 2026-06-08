package me.whereareiam.identica.provider.credential.database.migration;

import me.whereareiam.identica.adapter.database.DefaultDatabaseService;
import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.model.config.persistence.H2Persistence;
import me.whereareiam.identica.provider.credential.database.CredentialSchemaContributor;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Rename cracked provider id migration")
class V2RenameCrackedProviderIdTest {
	private DefaultDatabaseService databaseService;
	private Jdbi jdbi;

	@BeforeEach
	void setUp() throws Exception {
		Path dataPath = Files.createTempDirectory("credential-provider-id-migration-");

		H2Persistence persistence = new H2Persistence();
		persistence.setFile("mem:credential_provider_id_migration;DB_CLOSE_DELAY=-1");
		persistence.setOptions("MODE=PostgreSQL");

		databaseService = new DefaultDatabaseService(
				persistence,
				Mockito.mock(EventManager.class),
				dataPath
		);
		jdbi = databaseService.getJdbi();
	}

	@AfterEach
	void tearDown() {
		if (databaseService != null)
			databaseService.onShutdown(null);
	}

	@Test
	void migratesLegacyCrackedProviderEntriesToCredential() {
		UUID uniqueId = UUID.randomUUID();

		jdbi.useHandle(handle -> {
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
			handle.execute("""
					CREATE TABLE identica_provider_credential_accounts_history (
						provider_id VARCHAR(64) NOT NULL,
						provider_subject VARCHAR(128) NOT NULL,
						hashing_method VARCHAR(64) NOT NULL,
						change_reason VARCHAR(32) NOT NULL,
						changed_at BIGINT,
						PRIMARY KEY (provider_id, provider_subject, changed_at),
						FOREIGN KEY (provider_id, provider_subject)
							REFERENCES identica_provider_credential_accounts(provider_id, provider_subject)
							ON DELETE CASCADE
					)
					""");
			handle.execute("""
					CREATE TABLE identica_username_history (
						unique_id UUID NOT NULL,
						provider_id VARCHAR(64),
						old_username VARCHAR(64) NOT NULL,
						new_username VARCHAR(64) NOT NULL,
						source VARCHAR(32) NOT NULL,
						changed_at BIGINT NOT NULL
					)
					""");
			handle.execute(
					"INSERT INTO identica_accounts (unique_id, username, created_at, last_seen_at) VALUES (?, ?, ?, ?)",
					uniqueId, "PlayerOne", 100L, 200L
			);
			handle.execute(
					"INSERT INTO identica_provider_links (unique_id, provider_id, provider_subject, is_primary, linked_at, last_seen_at) VALUES (?, ?, ?, ?, ?, ?)",
					uniqueId, "cracked", "offline-subject", true, 110L, 210L
			);
			handle.execute(
					"INSERT INTO identica_provider_profiles (provider_id, provider_subject, provider_username) VALUES (?, ?, ?)",
					"cracked", "offline-subject", "PlayerOne"
			);
			handle.execute(
					"INSERT INTO identica_provider_credential_accounts (provider_id, provider_subject, password_hash, hashing_method, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?)",
					"cracked", "offline-subject", "hash", "bcrypt", 120L, 220L
			);
			handle.execute(
					"INSERT INTO identica_provider_credential_accounts_history (provider_id, provider_subject, hashing_method, change_reason, changed_at) VALUES (?, ?, ?, ?, ?)",
					"cracked", "offline-subject", "bcrypt", "REGISTER", 130L
			);
			handle.execute(
					"INSERT INTO identica_verification_selections (unique_id, provider_id, method_id, selected_at) VALUES (?, ?, ?, ?)",
					uniqueId, "cracked", "password", 140L
			);
			handle.execute(
					"INSERT INTO identica_username_history (unique_id, provider_id, old_username, new_username, source, changed_at) VALUES (?, ?, ?, ?, ?, ?)",
					uniqueId, "cracked", "OldName", "PlayerOne", "MANUAL", 150L
			);
		});

		databaseService.apply(new CredentialSchemaContributor());

		assertEquals(0L, countRows("identica_provider_links", "cracked"));
		assertEquals(1L, countRows("identica_provider_links", "credential"));
		assertEquals(0L, countRows("identica_provider_profiles", "cracked"));
		assertEquals(1L, countRows("identica_provider_profiles", "credential"));
		assertEquals(0L, countRows("identica_provider_credential_accounts", "cracked"));
		assertEquals(1L, countRows("identica_provider_credential_accounts", "credential"));
		assertEquals(0L, countRows("identica_provider_credential_accounts_history", "cracked"));
		assertEquals(1L, countRows("identica_provider_credential_accounts_history", "credential"));
		assertEquals(0L, countRows("identica_verification_selections", "cracked"));
		assertEquals(1L, countRows("identica_verification_selections", "credential"));
		assertEquals(0L, countRows("identica_username_history", "cracked"));
		assertEquals(1L, countRows("identica_username_history", "credential"));
	}

	private long countRows(String tableName, String providerId) {
		return jdbi.withHandle(handle -> handle.createQuery(
						"SELECT COUNT(*) FROM %s WHERE provider_id = :providerId".formatted(tableName))
				.bind("providerId", providerId)
				.mapTo(Long.class)
				.one());
	}
}
