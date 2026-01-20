package me.whereareiam.identica.common;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import lombok.RequiredArgsConstructor;
import me.whereareiam.configura.Config;
import me.whereareiam.configura.reader.ConfigReader;
import me.whereareiam.configura.type.Format;
import me.whereareiam.configura.writer.ConfigWriter;
import me.whereareiam.identica.Reloadable;
import me.whereareiam.identica.common.config.provider.*;
import me.whereareiam.identica.common.config.resolver.FileSystemConfigurationTypeResolver;
import me.whereareiam.identica.common.auth.AuthPipeline;
import me.whereareiam.identica.common.event.EventController;
import me.whereareiam.identica.common.loader.dependency.ProviderDependencyResolver;
import me.whereareiam.identica.common.loader.reader.DefaultProviderDescriptorReader;
import me.whereareiam.identica.loader.ProviderDescriptorReader;
import me.whereareiam.identica.common.loader.DefaultProviderManager;
import me.whereareiam.identica.loader.ProviderManager;
import me.whereareiam.identica.common.provider.SerializerEngineProvider;
import me.whereareiam.identica.common.registry.ReloadableRegistry;
import me.whereareiam.identica.config.ConfigurationTypeResolver;
import me.whereareiam.identica.model.config.*;
import me.whereareiam.identica.model.config.persistence.Persistence;
import me.whereareiam.identica.registry.Registry;
import me.whereareiam.identica.Serializer;
import me.whereareiam.identica.event.EventManager;
import me.whereareiam.keystone.serializer.SerializerEngine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

@RequiredArgsConstructor
public class CommonConfiguration extends AbstractModule {
	private final Path dataPath;

	@Override
	protected void configure() {
		requestInjection(this);

		// Reloadables
		bind(new TypeLiteral<Registry<Reloadable>>() {
		}).to(ReloadableRegistry.class).asEagerSingleton();
		bind(new TypeLiteral<Set<Reloadable>>() {
		}).annotatedWith(Names.named("reloadables")).toProvider(ReloadableRegistry.class).asEagerSingleton();

		// Configuration
		bind(ConfigurationTypeResolver.class)
				.to(FileSystemConfigurationTypeResolver.class)
				.asEagerSingleton();

		// Configs
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
		// Services
		bind(SerializerEngine.class).toProvider(SerializerEngineProvider.class);
		bind(EventManager.class).to(EventController.class);

		// Plugin
		bind(Identica.class).asEagerSingleton();
		bind(ProviderManager.class).to(DefaultProviderManager.class).asEagerSingleton();
		bind(ProviderDescriptorReader.class).to(DefaultProviderDescriptorReader.class).asEagerSingleton();
		bind(ProviderDependencyResolver.class).asEagerSingleton();
		bind(AuthPipeline.class).asEagerSingleton();
	}

	@Inject
	void initializeSerializationHelper(Provider<SerializerEngine> serializerProvider) {
		Serializer.initialize(serializerProvider);
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
