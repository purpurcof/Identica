package me.whereareiam.identica.adapter.database.username;

import me.whereareiam.identica.adapter.database.repository.username.UsernameHistoryRepository;
import me.whereareiam.identica.adapter.database.testing.TestDataFactory;
import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.model.UsernameHistoryEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("Default Username-History Persistence Service")
class DefaultUsernameHistoryPersistenceServiceTest {
	@Mock
	private UsernameHistoryRepository repository;
	@Mock
	private EventManager eventManager;

	private DefaultUsernameHistoryPersistenceService service;

	@BeforeEach
	void setUp() {
		service = new DefaultUsernameHistoryPersistenceService(repository, eventManager);
	}

	@DisplayName("Rejects history entries with a blank previous username")
	@Test
	void recordRejectsBlankOldUsername() {
		UsernameHistoryEntry entry = TestDataFactory.usernameHistoryEntry(UUID.randomUUID(), "provider", " ", "new", "source");

		assertThrows(IllegalArgumentException.class, () -> service.record(entry));
	}

	@DisplayName("Rejects history entries with a blank new username")
	@Test
	void recordRejectsBlankNewUsername() {
		UsernameHistoryEntry entry = TestDataFactory.usernameHistoryEntry(UUID.randomUUID(), "provider", "old", " ", "source");

		assertThrows(IllegalArgumentException.class, () -> service.record(entry));
	}

	@DisplayName("Rejects history entries with a blank source")
	@Test
	void recordRejectsBlankSource() {
		UsernameHistoryEntry entry = TestDataFactory.usernameHistoryEntry(UUID.randomUUID(), "provider", "old", "new", " ");

		assertThrows(IllegalArgumentException.class, () -> service.record(entry));
	}

	@DisplayName("Persists username history entries through the repository")
	@Test
	void recordInsertsEntry() {
		UUID uniqueId = UUID.randomUUID();
		UsernameHistoryEntry entry = TestDataFactory.usernameHistoryEntry(uniqueId, "provider", "old", "new", "source");

		service.record(entry);

		verify(repository).insert(uniqueId, "provider", "old", "new", "source", TestDataFactory.CHANGED_AT);
	}

	@DisplayName("Deletes all username history entries for an account")
	@Test
	void deleteAllDelegatesToRepository() {
		UUID uniqueId = UUID.randomUUID();

		service.deleteAll(uniqueId);

		verify(repository).deleteAll(uniqueId);
	}
}
