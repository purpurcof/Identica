package me.whereareiam.identica.common.provider;

import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.identity.actor.ConnectionIdentity;
import me.whereareiam.identica.model.config.provider.Providers;
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
import me.whereareiam.identica.provider.profile.ProfileResolution;
import me.whereareiam.identica.provider.profile.ProfileResolveContext;
import me.whereareiam.identica.provider.profile.ProfileSubjectResolver;
import me.whereareiam.identica.type.pipeline.PipelineType;
import me.whereareiam.identica.type.pipeline.journey.JourneyMode;
import me.whereareiam.identica.type.pipeline.journey.StageType;
import me.whereareiam.identica.type.pipeline.journey.step.StepContextRequirement;
import me.whereareiam.identica.type.provider.ProviderState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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

	@DisplayName("Keeps automatic provider eligibility on untrusted IPs")
	@Test
	void keepsAutomaticEligibilityOnUntrustedIp() {
		Providers providers = new Providers();
		providers.setProviders(List.of(entry("alpha", 10, List.of("play.example.com"))));

		InternalProvider provider = enabledProvider();
		when(authenticationJourneyRegistry.resolvePlan(any(), any(), any(), any())).thenReturn(providerPlan());

		assertTrue(operations(providers).isEligible(autoContext(), provider, PipelineType.AUTHENTICATION, JourneyMode.INTERACTIVE));
	}

	@DisplayName("Returns no profile resolution when no provider resolver applies")
	@Test
	void resolveProfileReturnsNullWithoutProviderResolvers() {
		when(providerManager.getProviders()).thenReturn(List.of());

		ProfileResolution resolution = operations(new Providers()).resolveProfile(ProfileResolveContext.builder()
				.identity(new ConnectionIdentity(UUID.randomUUID(), "PlayerOne", "127.0.0.1"))
				.build());

		assertNull(resolution);
	}

	@DisplayName("Uses provider-registered profile resolvers to resolve subjects")
	@Test
	void resolveProfileUsesProviderResolvers() {
		ProviderDescriptor descriptor = new ProviderDescriptor();
		descriptor.setId("credential");

		InternalProvider provider = InternalProvider.builder()
				.descriptor(descriptor)
				.priority(10)
				.state(ProviderState.ENABLED)
				.profileSubjectResolvers(Set.of(new StaticProfileResolver()))
				.build();
		when(providerManager.getProviders()).thenReturn(List.of(provider));

		ProfileResolution resolution = operations(new Providers()).resolveProfile(ProfileResolveContext.builder()
				.identity(new ConnectionIdentity(UUID.randomUUID(), "PlayerOne", "127.0.0.1"))
				.build());

		assertNotNull(resolution);
		assertEquals("credential", resolution.getProviderId());
		assertEquals("credential-subject", resolution.getProviderSubject());
	}

	private ProviderOperations operations(Providers providers) {
		return new DefaultProviderOperations(
				providerManager,
				authenticationJourneyRegistry,
				registrationJourneyRegistry,
				migrationJourneyRegistry,
				() -> providers,
				eventManager
		);
	}

	private Providers.ProviderEntry entry(String id, int priority, List<String> entrypoints) {
		Providers.ProviderEntry entry = new Providers.ProviderEntry();
		entry.setId(id);
		entry.setPriority(priority);
		entry.setEntrypoints(entrypoints);
		return entry;
	}

	private InternalProvider enabledProvider() {
		ProviderDescriptor descriptor = new ProviderDescriptor();
		descriptor.setId("alpha");
		return InternalProvider.builder()
				.descriptor(descriptor)
				.priority(10)
				.state(ProviderState.ENABLED)
				.build();
	}

	private ScenarioContext autoContext() {
		return context();
	}

	private ScenarioContext context() {
		return new ScenarioContext() {
			private ProviderContext currentProvider = null;

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

	private static final class StaticProfileResolver implements ProfileSubjectResolver {
		@Override
		public boolean supports(@NotNull ProfileResolveContext context) {
			return true;
		}

		@Override
		public @Nullable ProfileResolution resolve(@NotNull ProfileResolveContext context) {
			return ProfileResolution.builder()
					.providerId("credential")
					.providerSubject("credential-subject")
					.build();
		}
	}
}
