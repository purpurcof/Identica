package me.whereareiam.identica.adapter.database.account;

import me.whereareiam.identica.adapter.database.entity.AccountEntity;
import me.whereareiam.identica.adapter.database.repository.account.AccountRepository;
import me.whereareiam.identica.adapter.database.testing.TestDataFactory;
import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.event.account.AccountClearEvent;
import me.whereareiam.identica.identity.actor.ConnectionIdentity;
import me.whereareiam.identica.model.identity.Account;
import me.whereareiam.identica.type.ClearScope;
import me.whereareiam.identica.type.UsernameSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultAccountPersistenceServiceTest {
	@Mock
	private AccountRepository accountRepository;
	@Mock
	private EventManager eventManager;

	private DefaultAccountPersistenceService service;

	@BeforeEach
	void setUp() {
		service = new DefaultAccountPersistenceService(accountRepository, eventManager);
	}

	@Test
	void findByUniqueIdMapsEntityToModel() {
		UUID uniqueId = UUID.randomUUID();
		AccountEntity entity = AccountEntity.builder()
				.uniqueId(uniqueId)
				.username("PlayerOne")
				.usernameSource("manual")
				.createdAt(TestDataFactory.CREATED_AT)
				.lastSeenAt(TestDataFactory.LAST_SEEN_AT)
				.build();
		when(accountRepository.findByUniqueId(uniqueId)).thenReturn(Optional.of(entity));

		Optional<Account> result = service.findByUniqueId(uniqueId);

		assertTrue(result.isPresent());
		assertEquals(uniqueId, result.get().getUniqueId());
		assertEquals("PlayerOne", result.get().getUsername());
		assertEquals(UsernameSource.MANUAL, result.get().getSource());
		assertEquals(TestDataFactory.CREATED_AT, result.get().getCreatedAt());
		assertEquals(TestDataFactory.LAST_SEEN_AT, result.get().getLastSeenAt());
	}

	@Test
	void findByUniqueIdReturnsEmptyOnException() {
		UUID uniqueId = UUID.randomUUID();
		when(accountRepository.findByUniqueId(uniqueId)).thenThrow(new RuntimeException("boom"));

		assertTrue(service.findByUniqueId(uniqueId).isEmpty());
	}

	@Test
	void findByUsernameReturnsEmptyForBlankInput() {
		List<Account> result = service.findByUsername("  ");

		assertTrue(result.isEmpty());
		verifyNoInteractions(accountRepository);
	}

	@Test
	void findByUsernameReturnsEmptyOnException() {
		when(accountRepository.findByUsername("Player")).thenThrow(new RuntimeException("boom"));

		List<Account> result = service.findByUsername("Player");

		assertTrue(result.isEmpty());
	}

	@Test
	void createDelegatesToRepository() {
		UUID uniqueId = UUID.randomUUID();
		Account account = TestDataFactory.account(uniqueId, "PlayerOne");

		Account created = service.create(account);

		verify(accountRepository).insert(
				uniqueId,
				"PlayerOne",
				UsernameSource.MANUAL.getId(),
				TestDataFactory.CREATED_AT,
				TestDataFactory.LAST_SEEN_AT
		);
		assertEquals(uniqueId, created.getUniqueId());
	}

	@Test
	void updateLastSeenSwallowsExceptions() {
		UUID uniqueId = UUID.randomUUID();
		doThrow(new RuntimeException("boom")).when(accountRepository).updateLastSeen(uniqueId, 123L);

		assertDoesNotThrow(() -> service.updateLastSeen(uniqueId, 123L));
	}

	@Test
	void updateUsernameIgnoresBlankInput() {
		service.updateUsername(UUID.randomUUID(), " ");

		verify(accountRepository, never()).updateUsername(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
	}

	@Test
	void updateUsernameCallsRepository() {
		UUID uniqueId = UUID.randomUUID();

		service.updateUsername(uniqueId, "PlayerTwo");

		verify(accountRepository).updateUsername(uniqueId, "PlayerTwo");
	}

	@Test
	void updateUsernameSourceDelegatesId() {
		UUID uniqueId = UUID.randomUUID();

		service.updateUsernameSource(uniqueId, UsernameSource.SYSTEM);

		verify(accountRepository).updateUsernameSource(uniqueId, UsernameSource.SYSTEM.getId());
	}

	@Test
	void deleteSwallowsExceptions() {
		UUID uniqueId = UUID.randomUUID();
		doThrow(new RuntimeException("boom")).when(accountRepository).delete(uniqueId);

		assertDoesNotThrow(() -> service.delete(uniqueId));
	}

	@Test
	void onAccountClearDeletesOnAllScope() {
		UUID uniqueId = UUID.randomUUID();
		ConnectionIdentity identity = new ConnectionIdentity(uniqueId, "Player", null);
		AccountClearEvent event = new AccountClearEvent(identity, ClearScope.ALL);

		service.onAccountClear(event);

		verify(accountRepository).delete(uniqueId);
	}

	@Test
	void onAccountClearIgnoresNonAllScope() {
		UUID uniqueId = UUID.randomUUID();
		ConnectionIdentity identity = new ConnectionIdentity(uniqueId, "Player", null);
		AccountClearEvent event = new AccountClearEvent(identity, ClearScope.CACHE);

		service.onAccountClear(event);

		verify(accountRepository, never()).delete(uniqueId);
	}

	@Test
	void onAccountClearIgnoresNullUniqueId() {
		ConnectionIdentity identity = new ConnectionIdentity(null, "Player", null);
		AccountClearEvent event = new AccountClearEvent(identity, ClearScope.ALL);

		service.onAccountClear(event);

		verify(accountRepository, never()).delete(org.mockito.ArgumentMatchers.any());
	}
}
