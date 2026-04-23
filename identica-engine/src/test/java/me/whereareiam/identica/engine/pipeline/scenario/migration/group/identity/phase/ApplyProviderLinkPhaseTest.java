package me.whereareiam.identica.engine.pipeline.scenario.migration.group.identity.phase;

import me.whereareiam.identica.database.provider.ProviderLinkPersistenceService;
import me.whereareiam.identica.database.provider.ProviderProfilePersistenceService;
import me.whereareiam.identica.engine.pipeline.scenario.migration.group.identity.IdentityState;
import me.whereareiam.identica.model.identity.Account;
import me.whereareiam.identica.model.identity.provider.AccountProviderLink;
import me.whereareiam.identica.model.identity.provider.AccountProviderProfile;
import me.whereareiam.identica.model.pipeline.PipelineResult;
import me.whereareiam.identica.model.pipeline.state.PipelineState;
import me.whereareiam.identica.type.UsernameSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Apply-Provider-Link Phase")
class ApplyProviderLinkPhaseTest {
	@Mock
	private ProviderLinkPersistenceService providerLinkPersistenceService;
	@Mock
	private ProviderProfilePersistenceService providerProfilePersistenceService;

	@DisplayName("Links the premium profile back to the existing account UUID")
	@Test
	void linksPremiumProviderToExistingAccountUuid() {
		UUID existingAccountUniqueId = UUID.randomUUID();
		Account account = Account.builder()
				.uniqueId(existingAccountUniqueId)
				.username("MigratedPlayer")
				.source(UsernameSource.PROVIDER)
				.build();
		AccountProviderProfile profile = AccountProviderProfile.builder()
				.providerId("premium")
				.providerSubject("premium-subject")
				.providerUsername("MigratedPlayer")
				.build();

		when(providerLinkPersistenceService.findBySubject("premium", "premium-subject"))
				.thenReturn(Optional.empty());
		when(providerLinkPersistenceService.upsert(any()))
				.thenAnswer(invocation -> invocation.getArgument(0));

		ApplyProviderLinkPhase phase = new ApplyProviderLinkPhase(
				providerLinkPersistenceService,
				providerProfilePersistenceService,
				me.whereareiam.identica.model.config.Messages::new
		);
		IdentityState state = new IdentityState();
		state.setResult(PipelineResult.complete());
		state.setAccount(account);
		state.setProfile(profile);

		phase.execute(PipelineState.initial(), state).toCompletableFuture().join();

		ArgumentCaptor<AccountProviderLink> linkCaptor = ArgumentCaptor.forClass(AccountProviderLink.class);
		verify(providerLinkPersistenceService).upsert(linkCaptor.capture());
		verify(providerProfilePersistenceService).upsert(profile);
		verify(providerLinkPersistenceService).setPrimaryExclusive(existingAccountUniqueId, "premium");

		AccountProviderLink storedLink = linkCaptor.getValue();
		assertEquals(existingAccountUniqueId, storedLink.getUniqueId());
		assertEquals("premium", storedLink.getProviderId());
		assertEquals("premium-subject", storedLink.getProviderSubject());
		assertNotNull(state.getLink());
		assertEquals(existingAccountUniqueId, state.getLink().getUniqueId());
		assertEquals("premium", state.getLink().getProviderId());
	}
}
