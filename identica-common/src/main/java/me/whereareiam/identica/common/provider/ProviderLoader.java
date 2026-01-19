package me.whereareiam.identica.common.provider;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import me.whereareiam.identica.logging.Logger;
import me.whereareiam.identica.model.ProviderDescriptor;
import me.whereareiam.identica.model.provider.ProviderLibraries;
import me.whereareiam.identica.provider.IdenticaProvider;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;

@Singleton
public class ProviderLoader {
	private final Path providersPath;
	private final ProviderDependencyResolver dependencyResolver;

	@Inject
	public ProviderLoader(
			@Named("providersPath") Path providersPath,
			ProviderDependencyResolver dependencyResolver
	) {
		this.providersPath = providersPath;
		this.dependencyResolver = dependencyResolver;
	}

	public boolean load(InternalProvider internal) {
		try {
			Path workingPath = ensureWorkingPath(resolveWorkingDirectoryName(internal.getDescriptor()));
			URLClassLoader classLoader = new URLClassLoader(
					new URL[]{internal.getPath().toUri().toURL()},
					getClass().getClassLoader()
			);

			ProviderLibraries descriptorLibraries = internal.getDescriptor().getLibraries();
			dependencyResolver.loadLibraries(internal.getDescriptor().getId(), descriptorLibraries, classLoader);

			Class<?> providerClass = classLoader.loadClass(internal.getDescriptor().getMain());
			Object instance = providerClass.getDeclaredConstructor().newInstance();
			if (!(instance instanceof IdenticaProvider provider)) {
				Logger.warn("Provider main class does not extend IdenticaProvider: %s", internal.getDescriptor().getId());
				internal.setState(ProviderState.FAILED);
				return false;
			}

			provider.setDescriptor(internal.getDescriptor());
			provider.setWorkingPath(workingPath);

			ProviderLibraries extraLibraries = provider.libraries();
			if (extraLibraries != null) {
				dependencyResolver.loadLibraries(internal.getDescriptor().getId(), extraLibraries, classLoader);
			}

			provider.onLoad();
			provider.onEnable();

			internal.setProvider(provider);
			internal.setWorkingPath(workingPath);
			internal.setClassLoader(classLoader);
			internal.setState(ProviderState.ENABLED);

			return true;
		} catch (Exception e) {
			internal.setState(ProviderState.FAILED);
			Logger.warn("Failed to load provider %s: %s", internal.getDescriptor().getId(), e.getMessage());

			return false;
		}
	}

	private Path ensureWorkingPath(String directoryName) {
		Path path = providersPath.resolve(directoryName);
		try {
			Files.createDirectories(path);
		} catch (Exception e) {
			throw new IllegalStateException("Failed to create provider working path: " + path, e);
		}

		return path;
	}

	private String resolveWorkingDirectoryName(ProviderDescriptor descriptor) {
		String name = descriptor != null ? descriptor.getName() : null;
		if (name == null || name.isBlank())
			name = descriptor != null ? descriptor.getId() : null;

		if (name == null || name.isBlank())
			return "unknown";

		return name.trim().replaceAll("[\\\\/:*?\"<>|]", "_");
	}
}
