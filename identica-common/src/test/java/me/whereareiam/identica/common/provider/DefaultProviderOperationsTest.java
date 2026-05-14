package me.whereareiam.identica.common.provider;

import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.identity.actor.ConnectionIdentity;
import me.whereareiam.identica.identity.session.recognition.policy.UntrustedIpRecognitionDecision;
import me.whereareiam.identica.identity.session.recognition.policy.UntrustedIpRecognitionPolicy;
import me.whereareiam.identica.model.config.Providers;
import me.whereareiam.identica.model.pipeline.ScenarioTransitionItem;
import me.whereareiam.identica.model.pipeline.journey.JourneyPlan;
import me.whereareiam.identica.model.pipeline.journey.stage.JourneyStage;
import me.whereareiam.identica.model.pipeline.journey.stage.step.JourneyStep;
import me.whereareiam.identica.model.pipeline.journey.stage.step.StepResult;
import me.whereareiam.identica.model.provider.InternalProvider;
import me.whereareiam.identica.model.provider.ProviderContext;
import me.whereareiam.identica.model.provider.ProviderDescriptor;
import me.whereareiam.identica.model.provider.ResolvedEntrypoint;
import me.whereareiam.identica.pipeline.ScenarioContext;
import me.whereareiam.identica.pipeline.journey.registry.type.AuthenticationJourneyRegistry;
import me.whereareiam.identica.pipeline.journey.registry.type.MigrationJourneyRegistry;
import me.whereareiam.identica.pipeline.journey.registry.type.RegistrationJourneyRegistry;
import me.whereareiam.identica.pipeline.journey.step.Step;
import me.whereareiam.identica.provider.ProviderManager;
import me.whereareiam.identica.provider.ProviderOperations;
import me.whereareiam.identica.type.pipeline.PipelineType;
import me.whereareiam.identica.type.pipeline.journey.JourneyMode;
import me.whereareiam.identica.type.pipeline.journey.StageType;
import me.whereareiam.identica.type.pipeline.journey.step.StepContextRequirement;
import me.whereareiam.identica.type.provider.ProviderOrigin;
import me.whereareiam.identica.type.provider.ProviderState;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Default Provider Operations")
class DefaultProviderOperationsTest {
	@Mock
	private ProviderManager providerManager;
	@Mock
	private AuthenticationJourneyRegistry authenticationJourneyRegistry;
	@Mock
	private RegistrationJourneyRegistry registrationJourneyRegistry;
	@Mock
	private MigrationJourneyRegistry migrationJourneyRegistry;
	@Mock
	private EventManager eventManager;
	@Mock
	private UntrustedIpRecognitionPolicy untrustedIpRecognitionPolicy;

	@DisplayName("Matches entrypoints by host name regardless of case")
	@Test
	void resolvesExactHostMatch() {
		Providers providers = new Providers();
		providers.setProviders(List.of(entry("alpha", 10, List.of("play.example.com"))));

		ProviderOperations operations = operations(providers);
		ResolvedEntrypoint resolved =
				operations.resolveEntrypoint("Play.Example.Com", 25565);

		assertNotNull(resolved);
		assertEquals("alpha", resolved.getProviderId());
	}

	@DisplayName("Requires the configured port when an entrypoint includes one")
	@Test
	void resolvesHostAndPortMatch() {
		Providers providers = new Providers();
		providers.setProviders(List.of(entry("alpha", 10, List.of("game.example.com:25570"))));

		ProviderOperations operations = operations(providers);
		assertNull(operations.resolveEntrypoint("game.example.com", 25565));

		ResolvedEntrypoint resolved =
				operations.resolveEntrypoint("game.example.com", 25570);

		assertNotNull(resolved);
		assertEquals("alpha", resolved.getProviderId());
	}

	@DisplayName("Prefers higher-priority providers before falling back to provider ID ordering")
	@Test
	void resolvesHighestPriorityThenProviderId() {
		Providers providers = new Providers();
		Providers.ProviderEntry low = entry("beta", 50, List.of("shared.example.com"));
		Providers.ProviderEntry high = entry("alpha", 100, List.of("shared.example.com"));
		providers.setProviders(List.of(low, high));

		ProviderOperations operations = operations(providers);
		ResolvedEntrypoint resolved =
				operations.resolveEntrypoint("shared.example.com", 25565);

		assertNotNull(resolved);
		assertEquals("alpha", resolved.getProviderId());
	}

	@DisplayName("Uses the configured provider display name when present")
	@Test
	void resolvesConfiguredProviderDisplayName() {
		Providers providers = new Providers();
		Providers.ProviderEntry entry = entry("alpha", 10, List.of("play.example.com"));
		entry.setDisplayName("Alpha Network");
		providers.setProviders(List.of(entry));

		ProviderOperations operations = operations(providers);
		assertEquals("Alpha Network", operations.displayProviderName("alpha"));
	}

	@DisplayName("Falls back to the provider descriptor name when no display name is configured")
	@Test
	void fallsBackToDescriptorNameWhenConfiguredDisplayNameMissing() {
		Providers providers = new Providers();
		providers.setProviders(List.of(entry("alpha", 10, List.of("play.example.com"))));

		ProviderDescriptor descriptor = new ProviderDescriptor();
		descriptor.setId("alpha");
		descriptor.setName("Alpha Provider");

		InternalProvider provider = InternalProvider.builder()
				.descriptor(descriptor)
				.state(ProviderState.ENABLED)
				.build();
		when(providerManager.getProviders()).thenReturn(List.of(provider));

		ProviderOperations operations = operations(providers);
		assertEquals("Alpha Provider", operations.displayProviderName("alpha"));
	}

	@DisplayName("Suppresses automatic provider eligibility when untrusted IP recognition blocks it")
	@Test
	void suppressesAutomaticEligibilityOnUntrustedIp() {
		Providers providers = new Providers();
		providers.setProviders(List.of(entry("alpha", 10, List.of("play.example.com"))));

		InternalProvider provider = enabledProvider("alpha");
		when(authenticationJourneyRegistry.resolvePlan(any(), any(), any(), any())).thenReturn(providerPlan());
		when(untrustedIpRecognitionPolicy.evaluateAutomaticRecognition(
				org.mockito.ArgumentMatchers.eq("alpha"),
				org.mockito.ArgumentMatchers.eq("127.0.0.1"),
				org.mockito.ArgumentMatchers.isNull()
		)).thenReturn(blockedDecision());

		assertFalse(operations(providers).isEligible(autoContext(), provider, PipelineType.AUTHENTICATION, JourneyMode.INTERACTIVE));
	}

	@DisplayName("Keeps explicit provider selection eligible on untrusted IP")
	@Test
	void keepsExplicitSelectionEligibleOnUntrustedIp() {
		Providers providers = new Providers();
		providers.setProviders(List.of(entry("alpha", 10, List.of("play.example.com"))));

		InternalProvider provider = enabledProvider("alpha");
		when(authenticationJourneyRegistry.resolvePlan(any(), any(), any(), any())).thenReturn(providerPlan());
		when(untrustedIpRecognitionPolicy.evaluateAutomaticRecognition(
				org.mockito.ArgumentMatchers.eq("alpha"),
				org.mockito.ArgumentMatchers.eq("127.0.0.1"),
				any()
		)).thenReturn(allowedDecision());

		assertTrue(operations(providers).isEligible(
				context(ProviderContext.of("alpha", null, "PlayerOne", ProviderOrigin.MANUAL)),
				provider,
				PipelineType.AUTHENTICATION,
				JourneyMode.INTERACTIVE
		));
	}

	@DisplayName("Restores automatic recognition only for providers with the override")
	@Test
	void providerOverrideRestoresRecognitionOnlyForMatchingProvider() {
		Providers providers = new Providers();
		Providers.ProviderEntry alpha = entry("alpha", 10, List.of("alpha.example.com"));
		alpha.getOverrides().setAllowRecognitionOnUntrustedIp(true);
		Providers.ProviderEntry beta = entry("beta", 10, List.of("beta.example.com"));
		providers.setProviders(List.of(alpha, beta));

		InternalProvider alphaProvider = enabledProvider("alpha");
		InternalProvider betaProvider = enabledProvider("beta");
		when(authenticationJourneyRegistry.resolvePlan(any(), any(), any(), any())).thenReturn(providerPlan());
		when(untrustedIpRecognitionPolicy.evaluateAutomaticRecognition(
				org.mockito.ArgumentMatchers.eq("alpha"),
				org.mockito.ArgumentMatchers.eq("127.0.0.1"),
				org.mockito.ArgumentMatchers.isNull()
		)).thenReturn(allowedDecision());
		when(untrustedIpRecognitionPolicy.evaluateAutomaticRecognition(
				org.mockito.ArgumentMatchers.eq("beta"),
				org.mockito.ArgumentMatchers.eq("127.0.0.1"),
				org.mockito.ArgumentMatchers.isNull()
		)).thenReturn(blockedDecision());

		ProviderOperations operations = operations(providers);
		assertTrue(operations.isEligible(autoContext(), alphaProvider, PipelineType.AUTHENTICATION, JourneyMode.INTERACTIVE));
		assertFalse(operations.isEligible(autoContext(), betaProvider, PipelineType.AUTHENTICATION, JourneyMode.INTERACTIVE));
	}

	private ProviderOperations operations(Providers providers) {
		return new DefaultProviderOperations(
				providerManager,
				authenticationJourneyRegistry,
				registrationJourneyRegistry,
				migrationJourneyRegistry,
				() -> providers,
				eventManager,
				untrustedIpRecognitionPolicy
		);
	}

	private Providers.ProviderEntry entry(String id, int priority, List<String> entrypoints) {
		Providers.ProviderEntry entry = new Providers.ProviderEntry();
		entry.setId(id);
		entry.setPriority(priority);
		entry.setEntrypoints(entrypoints);
		return entry;
	}

	private InternalProvider enabledProvider(String id) {
		ProviderDescriptor descriptor = new ProviderDescriptor();
		descriptor.setId(id);
		return InternalProvider.builder()
				.descriptor(descriptor)
				.priority(10)
				.state(ProviderState.ENABLED)
				.build();
	}

	private UntrustedIpRecognitionDecision allowedDecision() {
		return new UntrustedIpRecognitionDecision(
				UntrustedIpRecognitionDecision.Outcome.ALLOWED_IP_NOT_MATCHED
		);
	}

	private UntrustedIpRecognitionDecision blockedDecision() {
		return new UntrustedIpRecognitionDecision(
				UntrustedIpRecognitionDecision.Outcome.BLOCKED_UNTRUSTED_IP
		);
	}

	private ScenarioContext autoContext() {
		return context(null);
	}

	private ScenarioContext context(ProviderContext provider) {
		return new ScenarioContext() {
			private ProviderContext currentProvider = provider;

			@Override
			public @NotNull ConnectionIdentity getIdentity() {
				return new ConnectionIdentity(UUID.randomUUID(), "PlayerOne", "127.0.0.1");
			}

			@Override
			public String getIntendedServer() {
				return null;
			}

			@Override
			public ProviderContext getProvider() {
				return currentProvider;
			}

			@Override
			public void setProvider(ProviderContext provider) {
				currentProvider = provider;
			}

			@Override
			public ScenarioTransitionItem getTransition() {
				return null;
			}

			@Override
			public void setTransition(ScenarioTransitionItem transition) {
			}
		};
	}

	private JourneyPlan providerPlan() {
		JourneyStage stage = JourneyStage.builder()
				.id(StageType.PROVIDER.id())
				.type(StageType.PROVIDER)
				.pipelineTypes(Set.of(PipelineType.AUTHENTICATION))
				.journeyModes(Set.of(JourneyMode.INTERACTIVE, JourneyMode.SEAMLESS))
				.build();
		JourneyStep step = JourneyStep.builder()
				.stageId(StageType.PROVIDER.id())
				.step(new NoopStep())
				.scenarios(Set.of(PipelineType.AUTHENTICATION))
				.journeyModes(Set.of(JourneyMode.INTERACTIVE, JourneyMode.SEAMLESS))
				.build();
		return new JourneyPlan(List.of(new JourneyPlan.StageEntry(stage, List.of(step))));
	}

	private static final class NoopStep implements Step {
		@Override
		public @NotNull String getName() {
			return "noop";
		}

		@Override
		public @NotNull Set<JourneyMode> journeyModes() {
			return Set.of(JourneyMode.INTERACTIVE, JourneyMode.SEAMLESS);
		}

		@Override
		public @NotNull StepContextRequirement contextRequirement() {
			return StepContextRequirement.LOGIN;
		}

		@Override
		public @NotNull CompletableFuture<StepResult> execute(@NotNull ScenarioContext context) {
			return CompletableFuture.completedFuture(StepResult.proceed(context));
		}
	}
}
