package me.whereareiam.identica.common.loader;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import me.whereareiam.identica.common.loader.dependency.ProviderDependencyResolver;
import me.whereareiam.identica.common.loader.factory.ProviderClassLoaderFactory;
import me.whereareiam.identica.common.loader.factory.ProviderInstanceFactory;
import me.whereareiam.identica.common.loader.injector.ProviderInjectorFactory;
import me.whereareiam.identica.common.loader.resolver.ProviderWorkingPathResolver;
import me.whereareiam.identica.common.loader.resolver.ProviderResolverRegistry;
import me.whereareiam.identica.loader.IdenticaProvider;
import me.whereareiam.identica.loader.resolver.ProviderResolver;
import me.whereareiam.identica.logging.Logger;
import me.whereareiam.identica.model.ProviderDescriptor;
import me.whereareiam.identica.model.provider.InternalProvider;
import me.whereareiam.identica.type.ProviderState;

import java.net.URLClassLoader;
import java.nio.file.Path;

@Singleton
public class ProviderLifecycleController {
	private final ProviderWorkingPathResolver workingPathResolver;
	private final ProviderClassLoaderFactory classLoaderFactory;
	private final ProviderDependencyResolver dependencyResolver;
	private final ProviderInjectorFactory injectorFactory;
	private final ProviderInstanceFactory instanceFactory;
	private final ProviderResolverRegistry resolverRegistry;

	@Inject
	public ProviderLifecycleController(
			ProviderWorkingPathResolver workingPathResolver,
			ProviderClassLoaderFactory classLoaderFactory,
			ProviderDependencyResolver dependencyResolver,
			ProviderInjectorFactory injectorFactory,
			ProviderInstanceFactory instanceFactory,
			ProviderResolverRegistry resolverRegistry
	) {
		this.workingPathResolver = workingPathResolver;
		this.classLoaderFactory = classLoaderFactory;
		this.dependencyResolver = dependencyResolver;
		this.injectorFactory = injectorFactory;
		this.instanceFactory = instanceFactory;
		this.resolverRegistry = resolverRegistry;
	}

	public void loadProvider(InternalProvider internal) {
		if (internal == null || internal.getState() != ProviderState.DISCOVERED) return;

		try {
			ProviderDescriptor descriptor = internal.getDescriptor();
			Path workingPath = workingPathResolver.resolve(descriptor);
			URLClassLoader classLoader = classLoaderFactory.create(internal.getPath());

			dependencyResolver.loadDescriptorLibraries(descriptor, classLoader);

			Class<?> providerClass = classLoader.loadClass(descriptor.getMain());
			if (!IdenticaProvider.class.isAssignableFrom(providerClass)) {
				Logger.warn("Provider main class does not extend IdenticaProvider: %s", descriptor.getId());
				internal.setState(ProviderState.FAILED);
				classLoaderFactory.close(classLoader);

				return;
			}

			IdenticaProvider probeProvider = instanceFactory.instantiateProvider(providerClass);
			if (probeProvider != null) {
				probeProvider.setDescriptor(descriptor);
				probeProvider.setWorkingPath(workingPath);
			}

			dependencyResolver.loadProviderLibraries(descriptor, probeProvider, classLoader);

			Injector providerInjector = injectorFactory.create(workingPath, descriptor, probeProvider);
			IdenticaProvider provider = instanceFactory.createInjectedProvider(providerInjector, providerClass, probeProvider);
			if (provider == null) {
				internal.setState(ProviderState.FAILED);
				classLoaderFactory.close(classLoader);
				return;
			}

			provider.setDescriptor(descriptor);
			provider.setWorkingPath(workingPath);

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
			classLoaderFactory.close(internal.getClassLoader());
		}
	}

	private boolean checkRequirements(InternalProvider provider) {
		for (ProviderResolver resolver : resolverRegistry.getAll()) {
			if (!resolver.resolve(provider)) {
				provider.setState(ProviderState.FAILED);
				return true;
			}
		}

		return false;
	}

	private String safeId(InternalProvider internal) {
		if (internal == null || internal.getDescriptor() == null) {
			return "unknown";
		}

		String id = internal.getDescriptor().getId();
		return id != null && !id.isBlank() ? id : "unknown";
	}
}
