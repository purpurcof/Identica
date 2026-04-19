package me.whereareiam.identica.adapter.database.provider;

import me.whereareiam.identica.adapter.database.entity.account.AccountProviderLinkEntity;
import me.whereareiam.identica.adapter.database.repository.provider.ProviderLinkRepository;
import me.whereareiam.identica.adapter.database.testing.TestDataFactory;
import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.event.account.AccountClearEvent;
import me.whereareiam.identica.identity.actor.ConnectionIdentity;
import me.whereareiam.identica.model.identity.provider.AccountProviderLink;
import me.whereareiam.identica.type.ClearScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultProviderLinkPersistenceServiceTest {
	@Mock
	private ProviderLinkRepository repository;
	@Mock
	private EventManager eventManager;

	private DefaultProviderLinkPersistenceService service;

	@BeforeEach
	void setUp() {
		service = new DefaultProviderLinkPersistenceService(repository, eventManager);
	}

	@Test
	void findBySubjectReturnsEmptyForBlankValues() {
		assertTrue(service.findBySubject("", "subject").isEmpty());
		assertTrue(service.findBySubject("provider", " ").isEmpty());
		verifyNoInteractions(repository);
	}

	@Test
	void findByUniqueIdAndProviderIdReturnsEmptyForBlankProviderId() {
		UUID uniqueId = UUID.randomUUID();
		assertTrue(service.findByUniqueIdAndProviderId(uniqueId, " ").isEmpty());
		verifyNoInteractions(repository);
	}

	@Test
	void upsertRejectsBlankProviderId() {
		AccountProviderLink link = TestDataFactory.providerLink(UUID.randomUUID(), " ", "subject", false);

		assertThrows(IllegalArgumentException.class, () -> service.upsert(link));
	}

	@Test
	void upsertRejectsBlankProviderSubject() {
		AccountProviderLink link = TestDataFactory.providerLink(UUID.randomUUID(), "provider", " ", false);

		assertThrows(IllegalArgumentException.class, () -> service.upsert(link));
	}

	@Test
	void upsertUpdatesExistingLink() {
		UUID uniqueId = UUID.randomUUID();
		AccountProviderLink link = TestDataFactory.providerLink(uniqueId, "provider", "subject", true)
				.toBuilder()
				.lastSeenAt(999L)
				.build();
		AccountProviderLinkEntity existing = AccountProviderLinkEntity.builder()
				.uniqueId(uniqueId)
				.providerId("provider")
				.providerSubject("subject")
				.primaryLink(false)
				.linkedAt(TestDataFactory.LINKED_AT)
				.lastSeenAt(123L)
				.build();
		when(repository.findBySubject("provider", "subject")).thenReturn(Optional.of(existing));

		AccountProviderLink result = service.upsert(link);

		verify(repository).update("provider", "subject", true, 999L);
		verify(repository).updatePrimary(uniqueId, "provider", true);
		assertTrue(result.isPrimaryLink());
		assertEquals(999L, result.getLastSeenAt());
	}

	@Test
	void upsertInsertsNewLink() {
		UUID uniqueId = UUID.randomUUID();
		AccountProviderLink link = TestDataFactory.providerLink(uniqueId, "provider", "subject", false);
		when(repository.findBySubject("provider", "subject")).thenReturn(Optional.empty());

		AccountProviderLink result = service.upsert(link);

		verify(repository).insert(uniqueId, "provider", "subject", false, TestDataFactory.LINKED_AT, TestDataFactory.LAST_SEEN_AT);
		verify(repository, never()).updatePrimary(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyBoolean());
		assertEquals(uniqueId, result.getUniqueId());
	}

	@Test
	void deleteAllDelegatesToRepository() {
		UUID uniqueId = UUID.randomUUID();

		service.deleteAll(uniqueId);

		verify(repository).deleteAll(uniqueId);
	}

	@Test
	void onAccountClearDeletesOnAllScope() {
		UUID uniqueId = UUID.randomUUID();
		ConnectionIdentity identity = new ConnectionIdentity(uniqueId, "Player", null);
		AccountClearEvent event = new AccountClearEvent(identity, ClearScope.ALL);

		service.onAccountClear(event);

		verify(repository).deleteAll(uniqueId);
	}

	@Test
	void onAccountClearIgnoresNonAllScope() {
		UUID uniqueId = UUID.randomUUID();
		ConnectionIdentity identity = new ConnectionIdentity(uniqueId, "Player", null);
		AccountClearEvent event = new AccountClearEvent(identity, ClearScope.CACHE);

		service.onAccountClear(event);

		verify(repository, never()).deleteAll(uniqueId);
	}

	@Test
	void onAccountClearIgnoresNullUniqueId() {
		ConnectionIdentity identity = new ConnectionIdentity(null, "Player", null);
		AccountClearEvent event = new AccountClearEvent(identity, ClearScope.ALL);

		service.onAccountClear(event);

		verify(repository, never()).deleteAll(org.mockito.ArgumentMatchers.any());
	}
}
