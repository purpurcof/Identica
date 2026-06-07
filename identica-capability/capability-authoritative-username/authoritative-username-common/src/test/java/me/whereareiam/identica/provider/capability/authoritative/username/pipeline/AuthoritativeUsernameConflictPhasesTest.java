package me.whereareiam.identica.provider.capability.authoritative.username.pipeline;

import me.whereareiam.identica.database.AccountPersistenceService;
import me.whereareiam.identica.database.provider.ProviderLinkPersistenceService;
import me.whereareiam.identica.database.provider.ProviderProfilePersistenceService;
import me.whereareiam.identica.identity.actor.ConnectionIdentity;
import me.whereareiam.identica.model.auth.AuthContext;
import me.whereareiam.identica.model.identity.Account;
import me.whereareiam.identica.model.identity.provider.AccountProviderLink;
import me.whereareiam.identica.model.identity.provider.AccountProviderProfile;
import me.whereareiam.identica.model.pipeline.PipelineResult;
import me.whereareiam.identica.model.pipeline.prepare.PrepareAccountCandidateItem;
import me.whereareiam.identica.model.pipeline.prepare.PrepareContextItem;
import me.whereareiam.identica.model.pipeline.prepare.decision.PrepareDecisionItem;
import me.whereareiam.identica.model.provider.InternalProvider;
import me.whereareiam.identica.model.provider.ProviderContext;
import me.whereareiam.identica.model.provider.ProviderDescriptor;
import me.whereareiam.identica.model.registration.RegistrationContext;
import me.whereareiam.identica.pipeline.state.PipelineState;
import me.whereareiam.identica.pipeline.state.prepare.PrepareGroupState;
import me.whereareiam.identica.pipeline.state.scenario.type.registration.PolicyState;
import me.whereareiam.identica.provider.ProviderManager;
import me.whereareiam.identica.provider.capability.authoritative.username.conflict.resolver.UsernameConflictResolver;
import me.whereareiam.identica.provider.capability.authoritative.username.database.AccountUsernameStatePersistenceService;
import me.whereareiam.identica.provider.capability.authoritative.username.model.AuthoritativeUsernameMessages;
import me.whereareiam.identica.provider.capability.authoritative.username.model.conflict.UsernameConflictResult;
import me.whereareiam.identica.provider.capability.authoritative.username.pipeline.prepare.group.policy.phase.ApplyAuthoritativeUsernamePhase;
import me.whereareiam.identica.provider.capability.authoritative.username.pipeline.scenario.type.authentication.group.policy.phase.ReviewAuthenticationUsernamePhase;
import me.whereareiam.identica.provider.capability.authoritative.username.pipeline.scenario.type.registration.group.policy.phase.ReviewRegistrationUsernamePhase;
import me.whereareiam.identica.provider.capability.authoritative.username.type.AccountUsernameSource;
import me.whereareiam.identica.provider.capability.authoritative.username.type.AuthoritativeUsernameCapability;
import me.whereareiam.identica.type.pipeline.PipelineType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@DisplayName("Authoritative Username Conflict Phases")
class AuthoritativeUsernameConflictPhasesTest {
	@DisplayName("Prepare phase applies the handler result and stores a denial decision")
	@Test
	void preparePhaseAppliesHandlerResult() {
		UsernameConflictResolver resolver = mock(UsernameConflictResolver.class);
		when(resolver.handle(any(), eq("Player"))).thenReturn(
				UsernameConflictResult.denied("denied", "Resolved")
		);

		ApplyAuthoritativeUsernamePhase phase = new ApplyAuthoritativeUsernamePhase(
				providerManager(),
				statePersistenceService(),
				resolver,
				this::messages
		);

		PipelineState pipelineState = PipelineState.initial();
		pipelineState.putItem(new PrepareContextItem(providerContext(), null, null), 0L);
		pipelineState.putItem(candidate(), 0L);

		phase.execute(pipelineState, new PrepareGroupState()).toCompletableFuture().join();

		PrepareAccountCandidateItem candidate = pipelineState.item(PrepareAccountCandidateItem.class).orElseThrow();
		PrepareDecisionItem decision = pipelineState.item(PrepareDecisionItem.class).orElseThrow();
		assertEquals("Resolved", candidate.getEffectiveUsername());
		assertEquals("Resolved", candidate.getAccount().getUsername());
		assertEquals("denied", decision.getDenialMessage());
	}

	@DisplayName("Registration policy phase updates the identity username and denial state")
	@Test
	void registrationPhaseAppliesHandlerResult() {
		UsernameConflictResolver resolver = mock(UsernameConflictResolver.class);
		when(resolver.handle(any(), eq("Player"))).thenReturn(
				UsernameConflictResult.denied("denied", "Resolved")
		);

		ReviewRegistrationUsernamePhase phase = new ReviewRegistrationUsernamePhase(
				resolver,
				accountServiceEmpty(),
				linkServiceEmpty(),
				profileServiceEmpty()
		);

		PipelineState pipelineState = PipelineState.initial();
		pipelineState.setPipelineType(PipelineType.REGISTRATION);
		RegistrationContext context = RegistrationContext.builder()
				.identity(new ConnectionIdentity("Player", "127.0.0.1"))
				.provider(providerContext())
				.build();
		pipelineState.setScenario(context);
		pipelineState.putItem(snapshot(), 0L);

		PolicyState state = new PolicyState();
		state.setResult(PipelineResult.complete());
		phase.execute(pipelineState, state).toCompletableFuture().join();

		assertNotNull(state.getResult());
		assertEquals("denied", state.getResult().getMessage());
		verify(resolver).handle(any(), eq("Player"));
	}

	@DisplayName("Authentication policy phase updates the identity username")
	@Test
	void authenticationPhaseAppliesHandlerResult() {
		UsernameConflictResolver resolver = mock(UsernameConflictResolver.class);
		when(resolver.handle(any(), eq("Player"))).thenReturn(
				UsernameConflictResult.allowed("Resolved")
		);

		ReviewAuthenticationUsernamePhase phase = new ReviewAuthenticationUsernamePhase(
				resolver,
				accountServiceEmpty(),
				linkServiceEmpty(),
				profileServiceEmpty()
		);

		PipelineState pipelineState = PipelineState.initial();
		pipelineState.setPipelineType(PipelineType.AUTHENTICATION);
		AuthContext context = AuthContext.builder()
				.identity(new ConnectionIdentity("Player", "127.0.0.1"))
				.provider(providerContext())
				.build();
		pipelineState.setScenario(context);
		pipelineState.putItem(snapshot(), 0L);

		PolicyState state = new PolicyState();
		state.setResult(PipelineResult.complete());
		phase.execute(pipelineState, state).toCompletableFuture().join();

		verify(resolver).handle(any(), eq("Player"));
	}

	private PrepareAccountCandidateItem candidate() {
		PrepareAccountCandidateItem candidate = new PrepareAccountCandidateItem();
		candidate.setUniqueId(UUID.randomUUID());
		candidate.setAccount(Account.builder().uniqueId(candidate.getUniqueId()).username("Player").build());
		candidate.setLink(AccountProviderLink.builder()
				.uniqueId(candidate.getUniqueId())
				.providerId("premium")
				.providerSubject("subject")
				.primaryLink(true)
				.build());
		candidate.setProfile(AccountProviderProfile.builder()
				.providerId("premium")
				.providerSubject("subject")
				.providerUsername("Player")
				.build());
		return candidate;
	}

	private UsernameStateItem snapshot() {
		return UsernameStateItem.builder()
				.uniqueId(UUID.randomUUID())
				.providerId("premium")
				.providerSubject("subject")
				.source(AccountUsernameSource.PROVIDER)
				.build();
	}

	private ProviderLinkPersistenceService linkServiceEmpty() {
		ProviderLinkPersistenceService service = mock(ProviderLinkPersistenceService.class);
		when(service.findBySubject(anyString(), anyString())).thenReturn(Optional.empty());
		return service;
	}

	private ProviderProfilePersistenceService profileServiceEmpty() {
		ProviderProfilePersistenceService service = mock(ProviderProfilePersistenceService.class);
		when(service.findBySubject(anyString(), anyString())).thenReturn(Optional.empty());
		return service;
	}

	private AccountPersistenceService accountServiceEmpty() {
		AccountPersistenceService service = mock(AccountPersistenceService.class);
		when(service.findByUniqueId(any())).thenReturn(Optional.empty());
		return service;
	}

	private AccountUsernameStatePersistenceService statePersistenceService() {
		AccountUsernameStatePersistenceService service = mock(AccountUsernameStatePersistenceService.class);
		when(service.find(any())).thenReturn(Optional.empty());
		return service;
	}

	private ProviderManager providerManager() {
		ProviderManager providerManager = mock(ProviderManager.class);
		ProviderDescriptor descriptor = new ProviderDescriptor();
		descriptor.setId("premium");
		descriptor.setName("Premium");
		descriptor.setVersion("1");
		descriptor.setMain("Main");
		descriptor.setSupportedPlatforms(List.of("velocity"));
		descriptor.setDeclaredCapabilityIds(List.of(AuthoritativeUsernameCapability.CAPABILITY.getId()));

		InternalProvider provider = InternalProvider.builder()
				.descriptor(descriptor)
				.state(me.whereareiam.identica.type.provider.ProviderState.ENABLED)
				.build();
		when(providerManager.getProviders()).thenReturn(List.of(provider));
		return providerManager;
	}

	private ProviderContext providerContext() {
		return ProviderContext.builder()
				.providerId("premium")
				.providerSubject("subject")
				.providerUsername("Player")
				.build();
	}

	private AuthoritativeUsernameMessages messages() {
		AuthoritativeUsernameMessages.Pipeline.Prepare prepare = new AuthoritativeUsernameMessages.Pipeline.Prepare();
		prepare.setFailed(List.of("failed"));
		AuthoritativeUsernameMessages.Pipeline.Identity identity = new AuthoritativeUsernameMessages.Pipeline.Identity();
		identity.setSynchronizationFailed(List.of("sync"));
		AuthoritativeUsernameMessages.Pipeline.Policy policy = new AuthoritativeUsernameMessages.Pipeline.Policy();
		policy.setPersistenceFailed(List.of("persist"));
		policy.setConflictDenied(List.of("denied"));
		policy.setEntrypointRequired(List.of("entrypoint"));

		AuthoritativeUsernameMessages.Pipeline pipeline = new AuthoritativeUsernameMessages.Pipeline();
		pipeline.setPrepare(prepare);
		pipeline.setIdentity(identity);
		pipeline.setPolicy(policy);

		AuthoritativeUsernameMessages messages = new AuthoritativeUsernameMessages();
		messages.setPipeline(pipeline);
		return messages;
	}
}
