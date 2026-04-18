package me.whereareiam.identica.engine.prepare;

import me.whereareiam.identica.pipeline.prepare.PrepareStateStore;
import me.whereareiam.identica.database.AccountPersistenceService;
import me.whereareiam.identica.database.ProviderLinkPersistenceService;
import me.whereareiam.identica.database.ProviderProfilePersistenceService;
import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.event.account.AccountPrepareEvent;
import me.whereareiam.identica.event.base.Event;
import me.whereareiam.identica.identity.account.RegistrationAccountService;
import me.whereareiam.identica.identity.actor.ConnectionIdentity;
import me.whereareiam.identica.engine.pipeline.PipelineExecutor;
import me.whereareiam.identica.engine.prepare.group.context.ContextGroup;
import me.whereareiam.identica.engine.prepare.group.context.phase.ResolveEntrypointPhase;
import me.whereareiam.identica.engine.prepare.group.context.phase.RestorePrepareStatePhase;
import me.whereareiam.identica.engine.prepare.group.finalize.FinalizeGroup;
import me.whereareiam.identica.engine.prepare.group.finalize.phase.StorePrepareDecisionPhase;
import me.whereareiam.identica.engine.prepare.group.handshake.HandshakeGroup;
import me.whereareiam.identica.engine.prepare.group.handshake.phase.EvaluateHandshakePhase;
import me.whereareiam.identica.engine.prepare.group.handshake.phase.FinalizeHandshakePhase;
import me.whereareiam.identica.engine.prepare.group.policy.PolicyGroup;
import me.whereareiam.identica.engine.prepare.group.policy.phase.ApplyPreparePolicyPhase;
import me.whereareiam.identica.engine.prepare.group.profile.ProfileGroup;
import me.whereareiam.identica.engine.prepare.group.profile.phase.LoadPrepareAccountPhase;
import me.whereareiam.identica.engine.prepare.group.profile.phase.ResolveProfilePhase;
import me.whereareiam.identica.handshake.HandshakeStore;
import me.whereareiam.identica.model.auth.handshake.HandshakeDecision;
import me.whereareiam.identica.model.pipeline.prepare.decision.PrepareDecision;
import me.whereareiam.identica.model.pipeline.prepare.PrepareRequest;
import me.whereareiam.identica.type.PrepareStage;
import me.whereareiam.identica.model.config.Messages;
import me.whereareiam.identica.model.identity.Account;
import me.whereareiam.identica.model.identity.AccountDecision;
import me.whereareiam.identica.model.identity.provider.AccountProviderLink;
import me.whereareiam.identica.model.identity.provider.AccountProviderProfile;
import me.whereareiam.identica.model.provider.ResolvedEntrypoint;
import me.whereareiam.identica.provider.ProviderOperations;
import me.whereareiam.identica.provider.profile.ProfileResolution;
import me.whereareiam.identica.type.UsernameSource;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PreparePipelineTest {
	@Mock
	private HandshakeStore handshakeStore;
	@Mock
	private RegistrationAccountService registrationAccountService;
	@Mock
	private ProviderOperations providerOperations;
	@Mock
	private AccountPersistenceService accountPersistenceService;
	@Mock
	private ProviderLinkPersistenceService providerLinkPersistenceService;
	@Mock
	private ProviderProfilePersistenceService providerProfilePersistenceService;
	@Mock
	private EventManager eventManager;

	@Test
	void profileStageBuildsTransientAccountAndHonorsPrepareDecision() {
		UUID identicaUniqueId = UUID.randomUUID();
		ConnectionIdentity identity = identity("PlayerOne");
		TestPrepareStateStore prepareStateStore = new TestPrepareStateStore();
		PreparePipeline pipeline = pipeline(prepareStateStore);
		String connectionKey = "PlayerOne|127.0.0.1|premium.example.com|25565";
		identity.setObservedUniqueId(UUID.randomUUID());

		when(handshakeStore.policies()).thenReturn(java.util.Set.of());
		when(providerOperations.resolveProfile(any()))
				.thenReturn(ProfileResolution.builder()
						.providerId("premium")
						.providerSubject("premium-subject")
						.build());
		when(providerOperations.resolveEntrypoint("premium.example.com", 25565))
				.thenReturn(ResolvedEntrypoint.builder()
						.providerId("premium")
						.host("premium.example.com")
						.port(25565)
						.build());
		when(registrationAccountService.reserve(any())).thenReturn(identicaUniqueId);
		when(providerLinkPersistenceService.findBySubject("premium", "premium-subject"))
				.thenReturn(Optional.empty());
		when(accountPersistenceService.findByUniqueId(identicaUniqueId))
				.thenReturn(Optional.empty());
		when(providerProfilePersistenceService.findBySubject("premium", "premium-subject"))
				.thenReturn(Optional.empty());
		doAnswer(invocation -> {
			Event event = invocation.getArgument(0);
			if (event instanceof AccountPrepareEvent prepareEvent) {
				prepareEvent.setEffectiveUsername("PlayerOne*");
				prepareEvent.setDecision(AccountDecision.deny("use premium entrypoint"));
			}
			return null;
		}).when(eventManager).call(any());

		PrepareDecision decision = pipeline.execute(PrepareRequest.builder()
						.stage(PrepareStage.PROFILE)
						.connectionKey(connectionKey)
						.identity(identity)
						.build())
				.toCompletableFuture()
				.join();

		assertNotNull(decision);
		assertTrue(decision.isDenied());
		assertEquals("use premium entrypoint", decision.getDenialMessage());
		assertEquals("PlayerOne*", decision.getEffectiveUsername());
		assertEquals(identicaUniqueId, decision.getUniqueId());
		assertNotNull(decision.getProvider());
		assertEquals("premium", decision.getProvider().getProviderId());
		verify(eventManager).call(any(AccountPrepareEvent.class));
	}

	@Test
	void handshakeStageReusesPreviousHandshakeDecision() {
		TestPrepareStateStore prepareStateStore = new TestPrepareStateStore();
		PreparePipeline pipeline = pipeline(prepareStateStore);
		ConnectionIdentity identity = identity("PlayerTwo");
		String connectionKey = "PlayerTwo|127.0.0.1|premium.example.com|25565";
		PrepareDecision previous = PrepareDecision.builder()
				.status(PrepareDecision.Status.ALLOW)
				.handshake(HandshakeDecision.allow())
				.effectiveUsername("PlayerTwo")
				.build();
		prepareStateStore.put(connectionKey, previous);

		PrepareDecision decision = pipeline.execute(PrepareRequest.builder()
						.stage(PrepareStage.HANDSHAKE)
						.connectionKey(connectionKey)
						.identity(identity)
						.build())
				.toCompletableFuture()
				.join();

		assertNotNull(decision);
		assertEquals(PrepareDecision.Status.ALLOW, decision.getStatus());
		assertNotNull(decision.getHandshake());
	}

	@Test
	void profileStageReusesExistingLinkedUuidForPremiumJoin() {
		UUID identicaUniqueId = UUID.randomUUID();
		ConnectionIdentity identity = identity("MigratedPlayer");
		TestPrepareStateStore prepareStateStore = new TestPrepareStateStore();
		PreparePipeline pipeline = pipeline(prepareStateStore);
		String connectionKey = "MigratedPlayer|127.0.0.1|premium.example.com|25565";

		when(handshakeStore.policies()).thenReturn(java.util.Set.of());
		when(providerOperations.resolveProfile(any()))
				.thenReturn(ProfileResolution.builder()
						.providerId("premium")
						.providerSubject("premium-subject")
						.build());
		when(providerOperations.resolveEntrypoint("premium.example.com", 25565))
				.thenReturn(ResolvedEntrypoint.builder()
						.providerId("premium")
						.host("premium.example.com")
						.port(25565)
						.build());
		when(registrationAccountService.reserve(any())).thenReturn(identicaUniqueId);
		when(providerLinkPersistenceService.findBySubject("premium", "premium-subject"))
				.thenReturn(Optional.of(AccountProviderLink.builder()
						.uniqueId(identicaUniqueId)
						.providerId("premium")
						.providerSubject("premium-subject")
						.primaryLink(true)
						.build()));
		when(accountPersistenceService.findByUniqueId(identicaUniqueId))
				.thenReturn(Optional.of(Account.builder()
						.uniqueId(identicaUniqueId)
						.username("MigratedPlayer")
						.source(UsernameSource.PROVIDER)
						.build()));
		when(providerProfilePersistenceService.findBySubject("premium", "premium-subject"))
				.thenReturn(Optional.of(AccountProviderProfile.builder()
						.providerId("premium")
						.providerSubject("premium-subject")
						.providerUsername("MigratedPlayer")
						.build()));

		PrepareDecision decision = pipeline.execute(PrepareRequest.builder()
						.stage(PrepareStage.PROFILE)
						.connectionKey(connectionKey)
						.identity(identity)
						.build())
				.toCompletableFuture()
				.join();

		assertNotNull(decision);
		assertEquals(PrepareDecision.Status.ALLOW, decision.getStatus());
		assertEquals(identicaUniqueId, decision.getUniqueId());
		assertEquals("MigratedPlayer", decision.getEffectiveUsername());
		assertNotNull(prepareStateStore.peek(identicaUniqueId).orElse(null));
	}

	private @NotNull PreparePipeline pipeline(@NotNull PrepareStateStore prepareStateStore) {
		ConnectionProviderContextResolver contextResolver = new ConnectionProviderContextResolver(providerOperations);
		PreparePipelineRegistry registry = new PreparePipelineRegistry(
				new ContextGroup(),
				new HandshakeGroup(),
				new ProfileGroup(),
				new PolicyGroup(),
				new FinalizeGroup(),
				new RestorePrepareStatePhase(prepareStateStore),
				new ResolveEntrypointPhase(providerOperations, contextResolver),
				new EvaluateHandshakePhase(handshakeStore),
				new FinalizeHandshakePhase(eventManager, Messages::new),
				new ResolveProfilePhase(providerOperations, contextResolver),
				new LoadPrepareAccountPhase(
						registrationAccountService,
						accountPersistenceService,
						providerLinkPersistenceService,
						providerProfilePersistenceService
				),
				new ApplyPreparePolicyPhase(eventManager, Messages::new),
				new StorePrepareDecisionPhase(prepareStateStore)
		);
		return new PreparePipeline(registry, new PipelineExecutor());
	}

	private @NotNull ConnectionIdentity identity(@NotNull String username) {
		ConnectionIdentity identity = new ConnectionIdentity(username, "127.0.0.1");
		identity.setOrigin(new ConnectionIdentity.Origin("premium.example.com", 25565));
		return identity;
	}

	private static final class TestPrepareStateStore implements PrepareStateStore {
		private final Map<String, PrepareDecision> byKey = new HashMap<>();
		private final Map<UUID, PrepareDecision> byUniqueId = new HashMap<>();

		@Override
		public void put(@NotNull String connectionKey, @NotNull PrepareDecision decision) {
			byKey.put(connectionKey, decision);
		}

		@Override
		public void put(@NotNull UUID uniqueId, String connectionKey, @NotNull PrepareDecision decision) {
			byUniqueId.put(uniqueId, decision);
			if (connectionKey != null && !connectionKey.isBlank())
				byKey.put(connectionKey, decision);
		}

		@Override
		public @NotNull Optional<PrepareDecision> peek(@NotNull String connectionKey) {
			return Optional.ofNullable(byKey.get(connectionKey));
		}

		@Override
		public @NotNull Optional<PrepareDecision> peek(@NotNull UUID uniqueId) {
			return Optional.ofNullable(byUniqueId.get(uniqueId));
		}

		@Override
		public boolean clear(@NotNull UUID uniqueId) {
			return byUniqueId.remove(uniqueId) != null;
		}
	}
}
