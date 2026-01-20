package me.whereareiam.identica.common.loader;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import me.whereareiam.identica.common.loader.dependency.ProviderDependencyResolver;
import me.whereareiam.identica.common.loader.injector.ProviderInjectorConfiguration;
import me.whereareiam.identica.common.loader.resolver.ProviderPlatformResolver;
import me.whereareiam.identica.common.loader.resolver.ProviderResolver;
import me.whereareiam.identica.loader.IdenticaProvider;
import me.whereareiam.identica.logging.Logger;
import me.whereareiam.identica.model.ProviderDescriptor;
import me.whereareiam.identica.model.provider.InternalProvider;
import me.whereareiam.identica.model.provider.dependency.ProviderLibraries;
import me.whereareiam.identica.type.ProviderState;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Singleton
public class ProviderLifecycleController {
	private final Path providersPath;
	private final ProviderDependencyResolver dependencyResolver;
	private final Injector injector;
	private final List<ProviderResolver> resolvers;

	@Inject
	public ProviderLifecycleController(
			@Named("providersPath") Path providersPath,
			ProviderDependencyResolver dependencyResolver,
			Injector injector,
			ProviderPlatformResolver platformResolver
	) {
		this.providersPath = providersPath;
		this.dependencyResolver = dependencyResolver;
		this.injector = injector;
		this.resolvers = List.of(platformResolver);
	}

	public void loadProvider(InternalProvider internal) {
		if (internal == null || internal.getState() != ProviderState.DISCOVERED) return;

		try {
			ProviderDescriptor descriptor = internal.getDescriptor();
			Path workingPath = ensureWorkingPath(resolveWorkingDirectoryName(descriptor));
			URLClassLoader classLoader = new URLClassLoader(
					new URL[]{internal.getPath().toUri().toURL()},
					getClass().getClassLoader()
			);

			ProviderLibraries descriptorLibraries = descriptor.getLibraries();
			dependencyResolver.loadLibraries(descriptor.getId(), descriptorLibraries, classLoader);

			Class<?> providerClass = classLoader.loadClass(descriptor.getMain());
			Object instance = providerClass.getDeclaredConstructor().newInstance();
			if (!(instance instanceof IdenticaProvider provider)) {
				Logger.warn("Provider main class does not extend IdenticaProvider: %s", descriptor.getId());
				internal.setState(ProviderState.FAILED);
				closeClassLoader(classLoader);

				return;
			}

			provider.setDescriptor(descriptor);
			provider.setWorkingPath(workingPath);

			ProviderLibraries extraLibraries = provider.libraries();
			if (extraLibraries != null) {
				dependencyResolver.loadLibraries(descriptor.getId(), extraLibraries, classLoader);
			}

			List<Module> modules = new ArrayList<>();
			modules.add(new ProviderInjectorConfiguration(workingPath, descriptor));
			List<Module> providerModules = provider.modules();
			if (providerModules != null && !providerModules.isEmpty())
				modules.addAll(providerModules);

			Injector providerInjector = injector.createChildInjector(modules);
			providerInjector.injectMembers(provider);

			internal.setProvider(provider);
			internal.setWorkingPath(workingPath);
			internal.setClassLoader(classLoader);
			internal.setState(ProviderState.LOADED);

			if (checkRequirements(internal))
				return;

			provider.onLoad();
		} catch (Exception e) {
			internal.setState(ProviderState.FAILED);
			Logger.warn("Failed to load provider %s: %s", safeId(internal), e.getMessage());
		}
	}

	public void enableProvider(InternalProvider internal) {
		if (internal == null || internal.getState() != ProviderState.LOADED) return;
		if (internal.getProvider() == null) return;

		internal.setState(ProviderState.ENABLED);
		try {
			internal.getProvider().onEnable();
		} catch (Exception e) {
			internal.setState(ProviderState.FAILED);
			Logger.warn("Failed to enable provider %s: %s", safeId(internal), e.getMessage());
		}
	}

	public void disableProvider(InternalProvider internal) {
		if (internal == null || internal.getState() != ProviderState.ENABLED) return;
		if (internal.getProvider() == null) return;

		internal.setState(ProviderState.DISABLED);
		try {
			internal.getProvider().onDisable();
		} catch (Exception e) {
			internal.setState(ProviderState.FAILED);
			Logger.warn("Failed to disable provider %s: %s", safeId(internal), e.getMessage());
		}
	}

	public void unloadProvider(InternalProvider internal) {
		if (internal == null || internal.getState() != ProviderState.DISABLED) return;
		if (internal.getProvider() == null) return;

		try {
			internal.getProvider().onUnload();
			internal.setState(ProviderState.UNLOADED);
		} catch (Exception e) {
			internal.setState(ProviderState.FAILED);
			Logger.warn("Failed to unload provider %s: %s", safeId(internal), e.getMessage());
		} finally {
			closeClassLoader(internal.getClassLoader());
		}
	}

	private boolean checkRequirements(InternalProvider provider) {
		for (ProviderResolver resolver : resolvers) {
			if (!resolver.resolve(provider)) {
				provider.setState(ProviderState.FAILED);
				return true;
			}
		}

		return false;
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

	private void closeClassLoader(Object classLoader) {
		if (classLoader instanceof AutoCloseable closeable) {
			try {
				closeable.close();
			} catch (Exception e) {
				Logger.warn("Failed to close provider classloader: %s", e.getMessage());
			}
		}
	}

	private String safeId(InternalProvider internal) {
		if (internal == null || internal.getDescriptor() == null) {
			return "unknown";
		}

		String id = internal.getDescriptor().getId();
		return id != null && !id.isBlank() ? id : "unknown";
	}
}
