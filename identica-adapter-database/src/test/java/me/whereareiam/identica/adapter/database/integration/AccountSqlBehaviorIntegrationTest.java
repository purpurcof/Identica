package me.whereareiam.identica.adapter.database.integration;

import me.whereareiam.identica.adapter.database.testing.DatabaseFixture;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AccountSqlBehaviorIntegrationTest extends DatabaseIntegrationTestBase {
	@ParameterizedTest(name = "{0}")
	@MethodSource("fixtures")
	void findByUsernameIsCaseInsensitive(DatabaseFixture fixture) {
		fixture.reset();
		UUID uniqueId = UUID.randomUUID();
		String username = "CaseUser";

		List<UUID> results = fixture.withHandle(handle -> {
			handle.execute(
					"INSERT INTO identica_accounts (unique_id, username, username_source, created_at, last_seen_at) VALUES (?, ?, ?, ?, ?)",
					uniqueId,
					username,
					"manual",
					1L,
					2L
			);

			return handle.createQuery(
					"SELECT unique_id FROM identica_accounts WHERE LOWER(username) = LOWER(:username)"
			)
					.bind("username", "caseuser")
					.mapTo(UUID.class)
					.list();
		});

		assertEquals(List.of(uniqueId), results);
	}
}
