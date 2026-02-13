package me.whereareiam.identica.adapter.database.provider;

import me.whereareiam.identica.adapter.database.entity.AccountProviderProfileEntity;
import me.whereareiam.identica.adapter.database.repository.provider.ProviderProfileRepository;
import me.whereareiam.identica.adapter.database.testing.TestDataFactory;
import me.whereareiam.identica.model.identity.provider.AccountProviderProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultProviderProfilePersistenceServiceTest {
	@Mock
	private ProviderProfileRepository repository;

	private DefaultProviderProfilePersistenceService service;

	@BeforeEach
	void setUp() {
		service = new DefaultProviderProfilePersistenceService(repository);
	}

	@Test
	void findBySubjectReturnsEmptyForBlankValues() {
		assertTrue(service.findBySubject("", "subject").isEmpty());
		assertTrue(service.findBySubject("provider", " ").isEmpty());
		verifyNoInteractions(repository);
	}

	@Test
	void upsertRejectsBlankProviderId() {
		AccountProviderProfile profile = TestDataFactory.providerProfile(" ", "subject", "user");

		assertThrows(IllegalArgumentException.class, () -> service.upsert(profile));
	}

	@Test
	void upsertRejectsBlankProviderSubject() {
		AccountProviderProfile profile = TestDataFactory.providerProfile("provider", " ", "user");

		assertThrows(IllegalArgumentException.class, () -> service.upsert(profile));
	}

	@Test
	void upsertUpdatesExistingProfile() {
		AccountProviderProfile profile = TestDataFactory.providerProfile("provider", "subject", "user");
		AccountProviderProfileEntity existing = AccountProviderProfileEntity.builder()
				.providerId("provider")
				.providerSubject("subject")
				.providerUsername("old")
				.build();
		when(repository.findBySubject("provider", "subject")).thenReturn(Optional.of(existing));

		AccountProviderProfile result = service.upsert(profile);

		verify(repository).update("provider", "subject", "user");
		assertEquals("user", result.getProviderUsername());
	}

	@Test
	void upsertInsertsNewProfile() {
		AccountProviderProfile profile = TestDataFactory.providerProfile("provider", "subject", "user");
		when(repository.findBySubject("provider", "subject")).thenReturn(Optional.empty());

		AccountProviderProfile result = service.upsert(profile);

		verify(repository).insert("provider", "subject", "user");
		assertEquals("user", result.getProviderUsername());
	}

	@Test
	void deleteIgnoresBlankInputs() {
		service.delete(" ", "subject");
		service.delete("provider", " ");

		verify(repository, never()).delete(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
	}
}
