package me.whereareiam.identica.adapter.database.provider;

import me.whereareiam.identica.adapter.database.DefaultDatabaseService;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("Jdbi Provider")
class JdbiProviderTest {
	@DisplayName("Throws when the database service has not been initialized")
	@Test
	void throwsWhenDatabaseNotInitialized() {
		DefaultDatabaseService databaseService = mock(DefaultDatabaseService.class);
		when(databaseService.isInitialized()).thenReturn(false);

		JdbiProvider provider = new JdbiProvider(databaseService);

		assertThrows(IllegalStateException.class, provider::get);
	}

	@DisplayName("Returns the active Jdbi instance after initialization")
	@Test
	void returnsJdbiWhenInitialized() {
		DefaultDatabaseService databaseService = mock(DefaultDatabaseService.class);
		Jdbi jdbi = mock(Jdbi.class);
		when(databaseService.isInitialized()).thenReturn(true);
		when(databaseService.getJdbi()).thenReturn(jdbi);

		JdbiProvider provider = new JdbiProvider(databaseService);

		assertSame(jdbi, provider.get());
	}
}
