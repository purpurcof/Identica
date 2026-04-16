package me.whereareiam.identica.common;

import com.google.inject.*;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.multibindings.OptionalBinder;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import lombok.RequiredArgsConstructor;
import me.whereareiam.configura.Config;
import me.whereareiam.configura.node.Node;
import me.whereareiam.configura.reader.ConfigReader;
import me.whereareiam.configura.type.Format;
import me.whereareiam.configura.writer.ConfigWriter;
import me.whereareiam.identica.Registry;
import me.whereareiam.identica.Reloadable;
import me.whereareiam.identica.Serializer;
import me.whereareiam.identica.identity.account.RegistrationAccountService;
import me.whereareiam.identica.common.handshake.DefaultHandshakeStore;
import me.whereareiam.identica.common.identity.account.DefaultRegistrationAccountService;
import me.whereareiam.identica.common.migration.DefaultMigrationService;
import me.whereareiam.identica.common.replication.DefaultReplicationAdapter;
import me.whereareiam.identica.common.replication.DefaultReplicationSystem;
import me.whereareiam.identica.common.replication.NoopReplicationAdapter;
import me.whereareiam.identica.common.config.adapter.DateTimePatternAdapter;
import me.whereareiam.identica.common.config.adapter.DurationAdapter;
import me.whereareiam.identica.common.config.adapter.NodeAdapter;
import me.whereareiam.identica.common.config.adapter.ProviderCapabilityAdapter;
import me.whereareiam.identica.common.config.provider.*;
import me.whereareiam.identica.common.config.resolver.FileSystemConfigurationTypeResolver;
import me.whereareiam.identica.common.conflict.ConflictPrepareLifecycle;
import me.whereareiam.identica.common.conflict.DefaultConflictService;
import me.whereareiam.identica.common.conflict.type.UsernameConflictType;
import me.whereareiam.identica.conflict.ConflictGuard;
import me.whereareiam.identica.common.provider.DefaultProviderAttemptStore;
import me.whereareiam.identica.common.event.EventController;
import me.whereareiam.identica.common.identity.DefaultReservationCache;
import me.whereareiam.identica.common.listener.clear.AccountClearReplicationListener;
import me.whereareiam.identica.common.listener.clear.AccountClearSessionListener;
import me.whereareiam.identica.common.listener.DefaultDynamicListenerRegistry;
import me.whereareiam.identica.common.listener.SessionReplacedListener;
import me.whereareiam.identica.common.identity.DefaultIdentityService;
import me.whereareiam.identica.common.provider.DefaultProviderManager;
import me.whereareiam.identica.common.provider.DefaultProviderOperations;
import me.whereareiam.identica.common.provider.ProviderEntrypointSelectionLifecycle;
import me.whereareiam.identica.common.provider.SerializerEngineProvider;
import me.whereareiam.identica.common.prepare.DefaultPrepareStateStore;
import me.whereareiam.identica.common.provider.reader.DefaultProviderDescriptorReader;
import me.whereareiam.identica.common.registry.ReloadableRegistry;
import me.whereareiam.identica.common.sentinel.DefaultSentinelService;
import me.whereareiam.identica.common.sentinel.ConnectionAttemptSentinelLifecycle;
import me.whereareiam.identica.common.sentinel.SentinelRegistry;
import me.whereareiam.identica.common.sentinel.ResumeSpamSentinelDefinition;
import me.whereareiam.identica.common.routing.PhaseRoutingService;
import me.whereareiam.identica.common.routing.DefaultRoutingStateStore;
import me.whereareiam.identica.common.routing.RoutingLifecycle;
import me.whereareiam.identica.common.routing.RoutingTargetMissingListener;
import me.whereareiam.identica.common.identity.session.DefaultSessionService;
import me.whereareiam.identica.common.identity.session.SessionRefreshCoordinator;
import me.whereareiam.identica.pipeline.state.PrepareStateStore;
import me.whereareiam.identica.replication.ReplicationAdapter;
import me.whereareiam.identica.replication.ReplicationSystem;
import me.whereareiam.identica.config.ConfigurationTypeResolver;
import me.whereareiam.identica.conflict.ConflictService;
import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.identity.ReservationCache;
import me.whereareiam.identica.listener.DynamicListenerRegistry;
import me.whereareiam.identica.model.config.*;
import me.whereareiam.identica.model.config.persistence.Persistence;
import me.whereareiam.identica.identity.IdentityService;
import me.whereareiam.identica.provider.ProviderDescriptorReader;
import me.whereareiam.identica.provider.ProviderManager;
import me.whereareiam.identica.provider.ProviderOperations;
import me.whereareiam.identica.sentinel.SentinelService;
import me.whereareiam.identica.sentinel.SentinelDefinition;
import me.whereareiam.identica.routing.RoutingService;
import me.whereareiam.identica.routing.RoutingStateStore;
import me.whereareiam.identica.identity.session.SessionService;
import me.whereareiam.identica.service.MigrationService;
import me.whereareiam.identica.type.provider.ProviderCapability;
import me.whereareiam.identica.util.EventUtil;
import me.whereareiam.keystone.serializer.SerializerEngine;
import me.whereareiam.identica.handshake.HandshakeStore;
import me.whereareiam.identica.provider.ProviderAttemptStore;

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
		bind(ResumeSpamSentinelDefinition.class).asEagerSingleton();
		bind(new TypeLiteral<Registry<SentinelDefinition>>() {})
				.to(SentinelRegistry.class)
				.asEagerSingleton();
		bind(HandshakeStore.class).to(DefaultHandshakeStore.class).asEagerSingleton();
		bind(ProviderAttemptStore.class).to(DefaultProviderAttemptStore.class).asEagerSingleton();
		bind(PrepareStateStore.class).to(DefaultPrepareStateStore.class).asEagerSingleton();

		// Replication
		OptionalBinder.newOptionalBinder(binder(), Key.get(ReplicationAdapter.class, Names.named("replicationAdapter")))
				.setDefault()
				.to(NoopReplicationAdapter.class)
				.asEagerSingleton();

		bind(ReplicationAdapter.class).to(DefaultReplicationAdapter.class).asEagerSingleton();
		bind(ReplicationSystem.class).to(DefaultReplicationSystem.class).asEagerSingleton();
		bind(ReservationCache.class).to(DefaultReservationCache.class).asEagerSingleton();
		bind(SentinelService.class).to(DefaultSentinelService.class).asEagerSingleton();

		// Account + presence
		bind(RegistrationAccountService.class).to(DefaultRegistrationAccountService.class).asEagerSingleton();
		bind(MigrationService.class).to(DefaultMigrationService.class).asEagerSingleton();
		bind(IdentityService.class).to(DefaultIdentityService.class).asEagerSingleton();

		// Session lifecycle
		bind(SessionService.class).to(DefaultSessionService.class).asEagerSingleton();
		bind(SessionRefreshCoordinator.class).asEagerSingleton();

		// Routing state
		bind(RoutingStateStore.class).to(DefaultRoutingStateStore.class).asEagerSingleton();

		// Routing
		bind(RoutingService.class).to(PhaseRoutingService.class).asEagerSingleton();
		bind(RoutingLifecycle.class).asEagerSingleton();
		bind(RoutingTargetMissingListener.class).asEagerSingleton();

		// Conflict resolution
		Multibinder.newSetBinder(binder(), ConflictGuard.class);
		bind(ConflictService.class).to(DefaultConflictService.class).asEagerSingleton();
		bind(UsernameConflictType.class).asEagerSingleton();

		// Event listeners
		bind(AccountClearReplicationListener.class).asEagerSingleton();
		bind(AccountClearSessionListener.class).asEagerSingleton();
		bind(SessionReplacedListener.class).asEagerSingleton();
		bind(ConflictPrepareLifecycle.class).asEagerSingleton();
		bind(ProviderEntrypointSelectionLifecycle.class).asEagerSingleton();
		bind(ConnectionAttemptSentinelLifecycle.class).asEagerSingleton();
		bind(DynamicListenerRegistry.class).to(DefaultDynamicListenerRegistry.class).asEagerSingleton();

		// Provider system
		bind(ProviderDescriptorReader.class).to(DefaultProviderDescriptorReader.class).asEagerSingleton();
		bind(ProviderManager.class).to(DefaultProviderManager.class).asEagerSingleton();
		bind(ProviderOperations.class).to(DefaultProviderOperations.class).asEagerSingleton();

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
