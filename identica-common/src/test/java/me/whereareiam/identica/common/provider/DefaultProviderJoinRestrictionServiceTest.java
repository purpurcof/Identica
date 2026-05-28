package me.whereareiam.identica.common.provider;

import me.whereareiam.identica.common.provider.restriction.DefaultProviderJoinRestrictionService;
import me.whereareiam.identica.common.provider.restriction.ProviderJoinRestrictionToggleStore;
import me.whereareiam.identica.common.replication.DefaultReplicationSystem;
import me.whereareiam.identica.common.replication.ReplicationTestFixtures;
import me.whereareiam.identica.database.provider.ProviderLinkPersistenceService;
import me.whereareiam.identica.identity.actor.ConnectionIdentity;
import me.whereareiam.identica.identity.session.recognition.SessionRecognitionService;
import me.whereareiam.identica.model.config.provider.Providers;
import me.whereareiam.identica.model.config.Replication;
import me.whereareiam.identica.model.config.Settings;
import me.whereareiam.identica.model.identity.provider.AccountProviderLink;
import me.whereareiam.identica.model.provider.restriction.ProviderJoinRestrictionDecision;
import me.whereareiam.identica.model.provider.restriction.ProviderJoinRestrictionStatus;
import me.whereareiam.identica.type.provider.ProviderJoinRestrictionCondition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("Default Provider Join-Restriction Service")
class DefaultProviderJoinRestrictionServiceTest {
	private static final ConnectionIdentity.Origin ORIGIN =
			new ConnectionIdentity.Origin("premium.arcadeya.com", 25565);

	@DisplayName("Enable activates configured provider restriction")
	@Test
	void enableActivatesConfiguredRestriction() {
		DefaultProviderJoinRestrictionService service = new DefaultProviderJoinRestrictionService(
				this::providers,
				new TestToggleStore(),
				mock(SessionRecognitionService.class),
				mock(ProviderLinkPersistenceService.class)
		);

		ProviderJoinRestrictionStatus status = service.enable("premium");

		assertTrue(status.isActive());
		assertEquals(Set.of(ProviderJoinRestrictionCondition.RECOGNIZED), status.getAllow());
	}

	@DisplayName("Enable does not activate unconfigured provider restriction")
	@Test
	void enableFailsWithoutConfiguredRestriction() {
		DefaultProviderJoinRestrictionService service = new DefaultProviderJoinRestrictionService(
				this::providers,
				new TestToggleStore(),
				mock(SessionRecognitionService.class),
				mock(ProviderLinkPersistenceService.class)
		);

		ProviderJoinRestrictionStatus status = service.enable("credential");

		assertFalse(status.isActive());
		assertTrue(status.getAllow().isEmpty());
	}

	@DisplayName("Provider-only evaluation allows when restriction is inactive")
	@Test
	void providerOnlyEvaluationAllowsWhenInactive() {
		TestToggleStore store = new TestToggleStore();
		DefaultProviderJoinRestrictionService service = new DefaultProviderJoinRestrictionService(
				this::providers,
				store,
				mock(SessionRecognitionService.class),
				mock(ProviderLinkPersistenceService.class)
		);

		ProviderJoinRestrictionDecision decision = service.evaluate("premium");

		assertTrue(decision.isAllowed());
		assertFalse(decision.isActive());
		assertTrue(decision.isConfigured());
		assertEquals(Set.of(ProviderJoinRestrictionCondition.RECOGNIZED), decision.getAllow());
	}

	@DisplayName("Provider-only evaluation denies active restrictions that require context")
	@Test
	void providerOnlyEvaluationDeniesContextDependentRestrictions() {
		TestToggleStore store = new TestToggleStore();
		DefaultProviderJoinRestrictionService service = new DefaultProviderJoinRestrictionService(
				this::providers,
				store,
				mock(SessionRecognitionService.class),
				mock(ProviderLinkPersistenceService.class)
		);

		store.enable("premium");
		ProviderJoinRestrictionDecision decision = service.evaluate("premium");

		assertFalse(decision.isAllowed());
		assertTrue(decision.isActive());
		assertTrue(decision.isConfigured());
		assertEquals(Set.of(ProviderJoinRestrictionCondition.RECOGNIZED), decision.getAllow());
		assertTrue(decision.getMatchedConditions().isEmpty());
	}

	@DisplayName("Provider-only evaluation denies active deny-all restrictions")
	@Test
	void providerOnlyEvaluationDeniesEmptyAllowSet() {
		TestToggleStore store = new TestToggleStore();
		DefaultProviderJoinRestrictionService service = new DefaultProviderJoinRestrictionService(
				this::providers,
				store,
				mock(SessionRecognitionService.class),
				mock(ProviderLinkPersistenceService.class)
		);

		store.enable("deny-all");
		ProviderJoinRestrictionDecision decision = service.evaluate("deny-all");

		assertFalse(decision.isAllowed());
		assertTrue(decision.isActive());
		assertTrue(decision.isConfigured());
		assertTrue(decision.getAllow().isEmpty());
		assertTrue(decision.getMatchedConditions().isEmpty());
	}

	@DisplayName("Full evaluation allows recognized sessions")
	@Test
	void fullEvaluationAllowsRecognizedSessions() {
		TestToggleStore store = new TestToggleStore();
		SessionRecognitionService recognitionService = mock(SessionRecognitionService.class);
		when(recognitionService.matches("premium", "subject", "Player", "1.1.1.1", ORIGIN))
				.thenReturn(true);
		DefaultProviderJoinRestrictionService service = new DefaultProviderJoinRestrictionService(
				this::providers,
				store,
				recognitionService,
				mock(ProviderLinkPersistenceService.class)
		);

		store.enable("premium");
		ProviderJoinRestrictionDecision decision = service.evaluate("premium", "subject", "Player", "1.1.1.1", ORIGIN);

		assertTrue(decision.isAllowed());
		assertEquals(Set.of(ProviderJoinRestrictionCondition.RECOGNIZED), decision.getMatchedConditions());
	}

	@DisplayName("Full evaluation allows linked accounts")
	@Test
	void fullEvaluationAllowsLinkedAccounts() {
		TestToggleStore store = new TestToggleStore();
		SessionRecognitionService recognitionService = mock(SessionRecognitionService.class);
		when(recognitionService.matches("linked-only", "subject", "Player", "1.1.1.1", ORIGIN))
				.thenReturn(false);
		ProviderLinkPersistenceService linkPersistenceService = mock(ProviderLinkPersistenceService.class);
		when(linkPersistenceService.findBySubject("linked-only", "subject"))
				.thenReturn(Optional.of(AccountProviderLink.builder()
						.uniqueId(UUID.randomUUID())
						.providerId("linked-only")
						.providerSubject("subject")
						.primaryLink(true)
						.build()));
		DefaultProviderJoinRestrictionService service = new DefaultProviderJoinRestrictionService(
				this::providers,
				store,
				recognitionService,
				linkPersistenceService
		);

		store.enable("linked-only");
		ProviderJoinRestrictionDecision decision = service.evaluate("linked-only", "subject", "Player", "1.1.1.1", ORIGIN);

		assertTrue(decision.isAllowed());
		assertEquals(Set.of(ProviderJoinRestrictionCondition.LINKED), decision.getMatchedConditions());
	}

	@DisplayName("Full evaluation allows when any allowed condition matches")
	@Test
	void fullEvaluationAllowsAnyMatchedCondition() {
		TestToggleStore store = new TestToggleStore();
		SessionRecognitionService recognitionService = mock(SessionRecognitionService.class);
		when(recognitionService.matches("combined", "subject", "Player", "1.1.1.1", ORIGIN))
				.thenReturn(false);
		ProviderLinkPersistenceService linkPersistenceService = mock(ProviderLinkPersistenceService.class);
		when(linkPersistenceService.findBySubject("combined", "subject"))
				.thenReturn(Optional.of(AccountProviderLink.builder()
						.uniqueId(UUID.randomUUID())
						.providerId("combined")
						.providerSubject("subject")
						.primaryLink(true)
						.build()));
		DefaultProviderJoinRestrictionService service = new DefaultProviderJoinRestrictionService(
				this::providers,
				store,
				recognitionService,
				linkPersistenceService
		);

		store.enable("combined");
		ProviderJoinRestrictionDecision decision = service.evaluate("combined", "subject", "Player", "1.1.1.1", ORIGIN);

		assertTrue(decision.isAllowed());
		assertEquals(Set.of(ProviderJoinRestrictionCondition.LINKED), decision.getMatchedConditions());
	}

	@DisplayName("Full evaluation denies when no condition matches")
	@Test
	void fullEvaluationDeniesWhenNoConditionMatches() {
		TestToggleStore store = new TestToggleStore();
		SessionRecognitionService recognitionService = mock(SessionRecognitionService.class);
		when(recognitionService.matches("premium", "subject", "Player", "1.1.1.1", ORIGIN))
				.thenReturn(false);
		DefaultProviderJoinRestrictionService service = new DefaultProviderJoinRestrictionService(
				this::providers,
				store,
				recognitionService,
				mock(ProviderLinkPersistenceService.class)
		);

		store.enable("premium");
		ProviderJoinRestrictionDecision decision = service.evaluate("premium", "subject", "Player", "1.1.1.1", ORIGIN);

		assertFalse(decision.isAllowed());
		assertTrue(decision.getMatchedConditions().isEmpty());
	}

	@DisplayName("Full evaluation denies linked restriction when subject is missing")
	@Test
	void fullEvaluationDeniesLinkedRestrictionWithoutSubject() {
		TestToggleStore store = new TestToggleStore();
		SessionRecognitionService recognitionService = mock(SessionRecognitionService.class);
		when(recognitionService.matches("linked-only", null, "Player", "1.1.1.1", ORIGIN))
				.thenReturn(false);
		DefaultProviderJoinRestrictionService service = new DefaultProviderJoinRestrictionService(
				this::providers,
				store,
				recognitionService,
				mock(ProviderLinkPersistenceService.class)
		);

		store.enable("linked-only");
		ProviderJoinRestrictionDecision decision = service.evaluate("linked-only", null, "Player", "1.1.1.1", ORIGIN);

		assertFalse(decision.isAllowed());
		assertTrue(decision.getMatchedConditions().isEmpty());
	}

	private Providers providers() {
		Providers providers = new Providers();

		Providers.ProviderEntry premium = new Providers.ProviderEntry();
		premium.setId("premium");
		Providers.ProviderEntry.JoinRestriction active = new Providers.ProviderEntry.JoinRestriction();
		active.setEnabled(true);
		active.setAllow(List.of(ProviderJoinRestrictionCondition.RECOGNIZED));
		premium.setJoinRestriction(active);

		Providers.ProviderEntry credential = new Providers.ProviderEntry();
		credential.setId("credential");
		Providers.ProviderEntry.JoinRestriction inactive = new Providers.ProviderEntry.JoinRestriction();
		inactive.setEnabled(false);
		credential.setJoinRestriction(inactive);

		Providers.ProviderEntry denyAll = new Providers.ProviderEntry();
		denyAll.setId("deny-all");
		Providers.ProviderEntry.JoinRestriction denyAllRestriction =
				new Providers.ProviderEntry.JoinRestriction();
		denyAllRestriction.setEnabled(true);
		denyAllRestriction.setAllow(List.of());
		denyAll.setJoinRestriction(denyAllRestriction);

		Providers.ProviderEntry linkedOnly = new Providers.ProviderEntry();
		linkedOnly.setId("linked-only");
		Providers.ProviderEntry.JoinRestriction linkedOnlyRestriction =
				new Providers.ProviderEntry.JoinRestriction();
		linkedOnlyRestriction.setEnabled(true);
		linkedOnlyRestriction.setAllow(List.of(ProviderJoinRestrictionCondition.LINKED));
		linkedOnly.setJoinRestriction(linkedOnlyRestriction);

		Providers.ProviderEntry combined = new Providers.ProviderEntry();
		combined.setId("combined");
		Providers.ProviderEntry.JoinRestriction combinedRestriction =
				new Providers.ProviderEntry.JoinRestriction();
		combinedRestriction.setEnabled(true);
		combinedRestriction.setAllow(List.of(
				ProviderJoinRestrictionCondition.RECOGNIZED,
				ProviderJoinRestrictionCondition.LINKED
		));
		combined.setJoinRestriction(combinedRestriction);

		providers.setProviders(List.of(premium, credential, denyAll, linkedOnly, combined));
		return providers;
	}

	private static final class TestToggleStore extends ProviderJoinRestrictionToggleStore {
		private final Set<String> active = new HashSet<>();

		private TestToggleStore() {
			super(
					new DefaultReplicationSystem(new ReplicationTestFixtures.TestReplicationAdapter()), () -> {
						Replication replication = new Replication();
						Replication.Cache cache = new Replication.Cache();
						cache.setProviderJoinRestrictions("provider-join-restrictions");
						replication.setCache(cache);
						return replication;
					},
					() -> {
						Providers settings = new Providers();
						Providers.Behavior behavior = new Providers.Behavior();
						behavior.setJoinRestrictionToggleTtl(Duration.ofDays(365));
						settings.setBehavior(behavior);
						return settings;
					}
			);
		}

		@Override
		public boolean isActive(String providerId) {
			return active.contains(providerId.trim().toLowerCase(Locale.ROOT));
		}

		@Override
		public void enable(String providerId) {
			active.add(providerId.trim().toLowerCase(Locale.ROOT));
		}

		@Override
		public void disable(String providerId) {
			active.remove(providerId.trim().toLowerCase(Locale.ROOT));
		}
	}
}
