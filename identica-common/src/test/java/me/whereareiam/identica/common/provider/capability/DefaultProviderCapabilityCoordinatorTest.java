package me.whereareiam.identica.common.provider.capability;

import com.google.inject.*;
import com.google.inject.Module;
import com.google.inject.multibindings.Multibinder;
import me.whereareiam.identica.model.provider.InternalProvider;
import me.whereareiam.identica.model.provider.ProviderDescriptor;
import me.whereareiam.identica.model.provider.capability.ProviderCapabilityDescriptor;
import me.whereareiam.identica.provider.capability.ProviderCapabilityCoordinator;
import me.whereareiam.identica.provider.capability.ProviderCapabilityRegistry;
import me.whereareiam.identica.provider.capability.ProviderCapabilityServiceRegistry;
import me.whereareiam.identica.provider.capability.bootstrap.ProviderCapabilityBootstrap;
import me.whereareiam.identica.provider.capability.bootstrap.ProviderCapabilityGlobalInstallContext;
import me.whereareiam.identica.provider.capability.bootstrap.ProviderCapabilityLocalInstallContext;
import me.whereareiam.identica.provider.capability.contribution.ProviderCapabilityContribution;
import me.whereareiam.identica.type.provider.ProviderState;
import me.whereareiam.identica.type.provider.capability.ProviderCapability;
import me.whereareiam.identica.type.provider.capability.ProviderCapabilityScope;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DefaultProviderCapabilityCoordinatorTest {
	private static final ProviderCapability SAMPLE_CAPABILITY = ProviderCapability.of("sample");

	@Test
	void installsGlobalRuntimeOnceAndResolvesLocalModules(@TempDir Path tempDir) {
		Injector injector = Guice.createInjector(new CapabilityTestModule());
		ProviderCapabilityCoordinator coordinator = injector.getInstance(ProviderCapabilityCoordinator.class);
		ProviderCapabilityRegistry registry = injector.getInstance(ProviderCapabilityRegistry.class);
		ProviderCapabilityServiceRegistry serviceRegistry = injector.getInstance(ProviderCapabilityServiceRegistry.class);

		InternalProvider provider = provider("provider-a", tempDir);
		List<ProviderCapabilityBootstrap> bootstraps = coordinator.validateBootstraps(
				provider.getDescriptor(),
				List.of(SampleCapabilityBootstrap.INSTANCE)
		);
		coordinator.ensureGlobalInstallations(provider, bootstraps);

		assertNotNull(registry.findInstallation(SAMPLE_CAPABILITY));
		assertNotNull(serviceRegistry.resolve(SAMPLE_CAPABILITY, SampleGlobalService.class));

		List<Module> localModules = coordinator.localModules(provider, bootstraps);
		Injector providerInjector = injector.createChildInjector(localModules);
		assertEquals("provider-a", providerInjector.getInstance(SampleLocalService.class).providerId());

		InternalProvider nextProvider = provider("provider-b", tempDir);
		coordinator.ensureGlobalInstallations(nextProvider, bootstraps);
		assertEquals(1, registry.installations().size());
	}

	@Test
	void rejectsMissingRequiredContribution() {
		Injector injector = Guice.createInjector(new CapabilityTestModule());
		ProviderCapabilityCoordinator coordinator = injector.getInstance(ProviderCapabilityCoordinator.class);
		ProviderDescriptor descriptor = descriptor("provider-a");
		List<ProviderCapabilityBootstrap> bootstraps = coordinator.validateBootstraps(
				descriptor,
				List.of(RequiredContributionCapabilityBootstrap.INSTANCE)
		);

		IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
				coordinator.validateContributions(descriptor, bootstraps, Set.of()));

		assertTrue(exception.getMessage().contains("sample"));
	}

	@Test
	void resolvesContributionsFromProviderInjector() {
		Injector injector = Guice.createInjector(new CapabilityTestModule(), new ContributionModule());
		ProviderCapabilityCoordinator coordinator = injector.getInstance(ProviderCapabilityCoordinator.class);

		Set<ProviderCapabilityContribution> contributions = coordinator.resolveContributions(injector);

		assertEquals(1, contributions.size());
		assertEquals(SAMPLE_CAPABILITY, contributions.iterator().next().capability());
	}

	private static @NotNull InternalProvider provider(@NotNull String id, @NotNull Path workingPath) {
		return InternalProvider.builder()
				.descriptor(descriptor(id))
				.workingPath(workingPath)
				.state(ProviderState.DISCOVERED)
				.build();
	}

	private static @NotNull ProviderDescriptor descriptor(@NotNull String id) {
		ProviderDescriptor descriptor = new ProviderDescriptor();
		descriptor.setId(id);
		descriptor.setName(id);
		descriptor.setVersion("1.0.0");
		descriptor.setMain("ignored.Main");
		descriptor.setSupportedPlatforms(List.of("ANY"));
		descriptor.setCapabilities(List.of("sample"));
		return descriptor;
	}

	private static final class CapabilityTestModule extends AbstractModule {
		@Override
		protected void configure() {
			bind(ProviderCapabilityCoordinator.class).to(DefaultProviderCapabilityCoordinator.class).asEagerSingleton();
			bind(ProviderCapabilityRegistry.class).to(DefaultProviderCapabilityRegistry.class).asEagerSingleton();
			bind(ProviderCapabilityServiceRegistry.class).to(DefaultProviderCapabilityServiceRegistry.class).asEagerSingleton();
		}
	}

	private static final class ContributionModule extends AbstractModule {
		@Override
		protected void configure() {
			Multibinder.newSetBinder(binder(), ProviderCapabilityContribution.class)
					.addBinding()
					.to(SampleContribution.class);
		}
	}

	public interface SampleGlobalService {
		@NotNull String value();
	}

	public interface SampleLocalService {
		@NotNull String providerId();
	}

	private static final class SampleCapabilityBootstrap implements ProviderCapabilityBootstrap {
		private static final SampleCapabilityBootstrap INSTANCE = new SampleCapabilityBootstrap();

		@Override
		public @NotNull ProviderCapabilityDescriptor descriptor() {
			return ProviderCapabilityDescriptor.builder()
					.capability(SAMPLE_CAPABILITY)
					.scopes(Set.of(ProviderCapabilityScope.GLOBAL, ProviderCapabilityScope.LOCAL))
					.build();
		}

		@Override
		public @NotNull List<Module> globalModules(@NotNull ProviderCapabilityGlobalInstallContext context) {
			return List.of(new SampleGlobalModule());
		}

		@Override
		public @NotNull List<Module> localModules(@NotNull ProviderCapabilityLocalInstallContext context) {
			return List.of(new SampleLocalModule(context.getProviderId()));
		}
	}

	private static final class RequiredContributionCapabilityBootstrap implements ProviderCapabilityBootstrap {
		private static final RequiredContributionCapabilityBootstrap INSTANCE = new RequiredContributionCapabilityBootstrap();

		@Override
		public @NotNull ProviderCapabilityDescriptor descriptor() {
			return ProviderCapabilityDescriptor.builder()
					.capability(SAMPLE_CAPABILITY)
					.requiresContribution(true)
					.build();
		}
	}

	private static final class SampleGlobalModule extends AbstractModule {
		@Override
		protected void configure() {
			bind(SampleGlobalService.class).toInstance(() -> "global");
			bind(SampleGlobalRegistrar.class).asEagerSingleton();
		}
	}

	@Singleton
	private static final class SampleGlobalRegistrar {
		@Inject
		private SampleGlobalRegistrar(
				ProviderCapabilityServiceRegistry serviceRegistry,
				SampleGlobalService service
		) {
			serviceRegistry.register(SAMPLE_CAPABILITY, SampleGlobalService.class, service);
		}
	}

	private static final class SampleLocalModule extends AbstractModule {
		private final String providerId;

		private SampleLocalModule(String providerId) {
			this.providerId = providerId;
		}

		@Override
		protected void configure() {
			bind(SampleLocalService.class).toInstance(() -> providerId);
		}
	}

	@Singleton
	private static final class SampleContribution implements ProviderCapabilityContribution {
		@Override
		public @NotNull ProviderCapability capability() {
			return SAMPLE_CAPABILITY;
		}
	}
}
