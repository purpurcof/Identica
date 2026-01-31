package me.whereareiam.identica.common;

import com.google.inject.*;
import com.google.inject.multibindings.OptionalBinder;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import lombok.RequiredArgsConstructor;
import me.whereareiam.configura.Config;
import me.whereareiam.configura.node.Node;
import me.whereareiam.configura.reader.ConfigReader;
import me.whereareiam.configura.type.Format;
import me.whereareiam.configura.writer.ConfigWriter;
import me.whereareiam.identica.Reloadable;
import me.whereareiam.identica.Serializer;
import me.whereareiam.identica.auth.AuthenticationCoordinator;
import me.whereareiam.identica.auth.HandshakePolicy;
import me.whereareiam.identica.auth.step.registry.StepRegistry;
import me.whereareiam.identica.cache.CacheService;
import me.whereareiam.identica.common.auth.DefaultAuthenticationCoordinator;
import me.whereareiam.identica.common.auth.FlowCoordinator;
import me.whereareiam.identica.common.auth.handshake.HandshakePolicyRegistry;
import me.whereareiam.identica.common.auth.stage.DefaultStepStageRegistry;
import me.whereareiam.identica.common.auth.step.registry.DefaultStepRegistry;
import me.whereareiam.identica.common.cache.DefaultCacheService;
import me.whereareiam.identica.common.config.adapter.DateTimePatternAdapter;
import me.whereareiam.identica.common.config.adapter.DurationAdapter;
import me.whereareiam.identica.common.config.adapter.NodeAdapter;
import me.whereareiam.identica.common.config.adapter.ProviderCapabilityAdapter;
import me.whereareiam.identica.common.config.provider.*;
import me.whereareiam.identica.common.config.resolver.FileSystemConfigurationTypeResolver;
import me.whereareiam.identica.common.conflict.ConflictPrepareLifecycle;
import me.whereareiam.identica.common.conflict.DefaultConflictService;
import me.whereareiam.identica.common.conflict.type.UsernameConflictType;
import me.whereareiam.identica.common.connection.DefaultConnectionStateRegistry;
import me.whereareiam.identica.common.event.EventController;
import me.whereareiam.identica.common.extension.DefaultConnectionExtensions;
import me.whereareiam.identica.common.extension.DefaultIdentityExtensions;
import me.whereareiam.identica.common.extension.DefaultPreLoginExtensions;
import me.whereareiam.identica.common.identity.DefaultIdentityService;
import me.whereareiam.identica.common.identity.DefaultReservationCache;
import me.whereareiam.identica.common.listener.AccountClearListener;
import me.whereareiam.identica.common.listener.AccountClearSynchronizationListener;
import me.whereareiam.identica.common.listener.DefaultDynamicListenerRegistry;
import me.whereareiam.identica.common.provider.DefaultProviderManager;
import me.whereareiam.identica.common.provider.SerializerEngineProvider;
import me.whereareiam.identica.common.provider.eligibility.DefaultProviderEligibilityService;
import me.whereareiam.identica.common.provider.reader.DefaultProviderDescriptorReader;
import me.whereareiam.identica.common.registry.DefaultIdentityRegistry;
import me.whereareiam.identica.common.registry.ReloadableRegistry;
import me.whereareiam.identica.common.routing.PhaseRoutingService;
import me.whereareiam.identica.common.routing.RoutingLifecycle;
import me.whereareiam.identica.common.routing.RoutingTargetMissingListener;
import me.whereareiam.identica.common.session.DefaultSessionService;
import me.whereareiam.identica.common.session.SessionRefreshCoordinator;
import me.whereareiam.identica.common.synchronization.DefaultSynchronizationService;
import me.whereareiam.identica.common.synchronization.NoopSynchronizationService;
import me.whereareiam.identica.config.ConfigurationTypeResolver;
import me.whereareiam.identica.conflict.ConflictService;
import me.whereareiam.identica.connection.ConnectionExtensions;
import me.whereareiam.identica.connection.ConnectionStateRegistry;
import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.identity.IdentityService;
import me.whereareiam.identica.identity.ReservationCache;
import me.whereareiam.identica.identity.registry.IdentityExtensions;
import me.whereareiam.identica.identity.registry.IdentityRegistry;
import me.whereareiam.identica.listener.DynamicListenerRegistry;
import me.whereareiam.identica.model.config.*;
import me.whereareiam.identica.model.config.persistence.Persistence;
import me.whereareiam.identica.provider.ProviderDescriptorReader;
import me.whereareiam.identica.provider.ProviderManager;
import me.whereareiam.identica.provider.eligibility.ProviderEligibilityService;
import me.whereareiam.identica.registry.PreLoginExtensions;
import me.whereareiam.identica.registry.Registry;
import me.whereareiam.identica.routing.RoutingService;
import me.whereareiam.identica.service.SynchronizationService;
import me.whereareiam.identica.session.SessionService;
import me.whereareiam.identica.stage.StepStageRegistry;
import me.whereareiam.identica.type.provider.ProviderCapability;
import me.whereareiam.identica.util.EventUtil;
import me.whereareiam.keystone.serializer.SerializerEngine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Set;

@RequiredArgsConstructor
public class CommonConfiguration extends AbstractModule {
	private final Path dataPath;

	@Override
	protected void configure() {
		requestInjection(this);

		// Configuration core
		bind(ConfigurationTypeResolver.class)
				.to(FileSystemConfigurationTypeResolver.class)
				.asEagerSingleton();

		// Configuration providers
		bind(SettingsProvider.class).asEagerSingleton();
		bind(Settings.class).toProvider(SettingsProvider.class);
		bind(MessagesProvider.class).asEagerSingleton();
		bind(Messages.class).toProvider(MessagesProvider.class);
		bind(CommandsProvider.class).asEagerSingleton();
		bind(Commands.class).toProvider(CommandsProvider.class);
		bind(ProvidersProvider.class).asEagerSingleton();
		bind(Providers.class).toProvider(ProvidersProvider.class);
		bind(PersistenceProvider.class).asEagerSingleton();
		bind(Persistence.class).toProvider(PersistenceProvider.class);
		bind(ReplicationProvider.class).asEagerSingleton();
		bind(Replication.class).toProvider(ReplicationProvider.class);

		// Registries & reloadables
		bind(new TypeLiteral<Registry<Reloadable>>() {})
				.to(ReloadableRegistry.class)
				.asEagerSingleton();
		bind(new TypeLiteral<Set<Reloadable>>() {})
				.annotatedWith(Names.named("reloadables"))
				.toProvider(ReloadableRegistry.class)
				.asEagerSingleton();
		bind(new TypeLiteral<Registry<HandshakePolicy>>() {})
				.to(HandshakePolicyRegistry.class)
				.asEagerSingleton();

		// Synchronization + cache
		OptionalBinder.newOptionalBinder(binder(), Key.get(SynchronizationService.class, Names.named("synchronizationProvider")))
				.setDefault()
				.to(NoopSynchronizationService.class)
				.asEagerSingleton();

		bind(SynchronizationService.class).to(DefaultSynchronizationService.class).asEagerSingleton();
		bind(CacheService.class).to(DefaultCacheService.class).asEagerSingleton();
		bind(ReservationCache.class).to(DefaultReservationCache.class).asEagerSingleton();

		// Identity lifecycle
		bind(IdentityRegistry.class).to(DefaultIdentityRegistry.class).asEagerSingleton();
		bind(IdentityService.class).to(DefaultIdentityService.class).asEagerSingleton();

		// Session lifecycle
		bind(SessionService.class).to(DefaultSessionService.class).asEagerSingleton();
		bind(SessionRefreshCoordinator.class).asEagerSingleton();

		// Connection state
		bind(ConnectionStateRegistry.class).to(DefaultConnectionStateRegistry.class).asEagerSingleton();
		bind(ConnectionExtensions.class).to(DefaultConnectionExtensions.class).asEagerSingleton();
		bind(IdentityExtensions.class).to(DefaultIdentityExtensions.class).asEagerSingleton();
		bind(PreLoginExtensions.class).to(DefaultPreLoginExtensions.class).asEagerSingleton();

		// Authentication
		bind(FlowCoordinator.class).asEagerSingleton();
		bind(AuthenticationCoordinator.class).to(DefaultAuthenticationCoordinator.class).asEagerSingleton();
		bind(ProviderEligibilityService.class).to(DefaultProviderEligibilityService.class).asEagerSingleton();

		// Routing
		bind(RoutingService.class).to(PhaseRoutingService.class).asEagerSingleton();
		bind(RoutingLifecycle.class).asEagerSingleton();
		bind(RoutingTargetMissingListener.class).asEagerSingleton();

		// Conflict resolution
		bind(ConflictService.class).to(DefaultConflictService.class).asEagerSingleton();
		bind(UsernameConflictType.class).asEagerSingleton();

		// Event listeners
		bind(AccountClearListener.class).asEagerSingleton();
		bind(AccountClearSynchronizationListener.class).asEagerSingleton();
		bind(ConflictPrepareLifecycle.class).asEagerSingleton();
		bind(DynamicListenerRegistry.class).to(DefaultDynamicListenerRegistry.class).asEagerSingleton();

		// Provider system
		bind(ProviderDescriptorReader.class).to(DefaultProviderDescriptorReader.class).asEagerSingleton();
		bind(ProviderManager.class).to(DefaultProviderManager.class).asEagerSingleton();
		bind(StepRegistry.class).to(DefaultStepRegistry.class).asEagerSingleton();
		bind(StepStageRegistry.class).to(DefaultStepStageRegistry.class).asEagerSingleton();

		// Core services
		bind(EventManager.class).to(EventController.class);
		bind(SerializerEngine.class).toProvider(SerializerEngineProvider.class);
		bind(Identica.class).asEagerSingleton();
	}

	@Inject
	void initializeSerializationHelper(Provider<SerializerEngine> serializerProvider) {
		Serializer.initialize(serializerProvider);
	}

	@Inject
	void initializeEventUtil(EventManager eventManager) {
		EventUtil.initialize(eventManager);
	}

	@Inject
	void initializeConfigura(
			ConfigurationTypeResolver resolver
	) {
		Format format = resolver.getConfigurationType();
		ConfigReader reader = Config.getDefaultReader().withFormat(format);
		ConfigWriter writer = Config.getDefaultWriter().withFormat(format);
		Config.setReader(reader);
		Config.setWriter(writer);

		Config.registerAdapter(Node.class, new NodeAdapter());
		Config.registerAdapter(Duration.class, new DurationAdapter());
		Config.registerAdapter(DateTimePattern.class, new DateTimePatternAdapter());
		Config.registerAdapter(ProviderCapability.class, new ProviderCapabilityAdapter());
	}

	@Provides
	@Singleton
	@Named("dataPath")
	Path provideDataPath() {
		return ensureDirectory(dataPath, "data");
	}

	@Provides
	@Singleton
	@Named("providersPath")
	Path provideProvidersPath(@Named("dataPath") Path dataPath) {
		return ensureDirectory(dataPath.resolve("providers"), "providers");
	}

	private Path ensureDirectory(Path path, String label) {
		try {
			Files.createDirectories(path);
			return path;
		} catch (IOException e) {
			throw new RuntimeException("Failed to create " + label + " directory", e);
		}
	}
}
