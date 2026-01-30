package me.whereareiam.identica.common.provider;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.common.provider.dependency.ProviderDependencyResolver;
import me.whereareiam.identica.common.provider.factory.ProviderClassLoaderFactory;
import me.whereareiam.identica.common.provider.factory.ProviderInstanceFactory;
import me.whereareiam.identica.common.provider.injector.ProviderInjectorFactory;
import me.whereareiam.identica.common.provider.resolver.ProviderWorkingPathResolver;
import me.whereareiam.identica.common.provider.resolver.ProviderResolverRegistry;
import me.whereareiam.identica.conflict.ConflictService;
import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.event.provider.ProviderDisabledEvent;
import me.whereareiam.identica.event.provider.ProviderEnabledEvent;
import me.whereareiam.identica.event.provider.ProviderLoadedEvent;
import me.whereareiam.identica.event.provider.ProviderUnloadedEvent;
import me.whereareiam.identica.provider.IdenticaProvider;
import me.whereareiam.identica.provider.resolver.ProviderResolver;
import me.whereareiam.identica.logging.Logger;
import me.whereareiam.identica.model.provider.ProviderDescriptor;
import me.whereareiam.identica.model.provider.InternalProvider;
import me.whereareiam.identica.type.provider.ProviderState;

import java.net.URLClassLoader;
import java.nio.file.Path;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ProviderLifecycleController {
	private final ProviderWorkingPathResolver workingPathResolver;
	private final ProviderClassLoaderFactory classLoaderFactory;
	private final ProviderDependencyResolver dependencyResolver;
	private final ProviderInjectorFactory injectorFactory;
	private final ProviderInstanceFactory instanceFactory;
	private final ProviderResolverRegistry resolverRegistry;
	private final ConflictService conflictService;
	private final EventManager eventManager;

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

			IdenticaProvider provider = instanceFactory.createInjectedProvider(
					injectorFactory.create(workingPath, descriptor, probeProvider),
					providerClass,
					probeProvider
			);
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
			fireProviderLoaded(internal);
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
			registerConflictResolvers(internal.getProvider());
			internal.getProvider().onEnable();
			fireProviderEnabled(internal);
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
			unregisterConflictResolvers(internal.getProvider());
			internal.getProvider().onDisable();
			fireProviderDisabled(internal);
		} catch (Exception e) {
			internal.setState(ProviderState.FAILED);
			Logger.warn("Failed to disable provider %s: %s", safeId(internal), e.getMessage());
			fireProviderDisabled(internal);
		}
	}

	public void unloadProvider(InternalProvider internal) {
		if (internal == null || internal.getState() != ProviderState.DISABLED) return;
		if (internal.getProvider() == null) return;

		try {
			internal.getProvider().onUnload();
			internal.setState(ProviderState.UNLOADED);
			fireProviderUnloaded(internal);
		} catch (Exception e) {
			internal.setState(ProviderState.FAILED);
			Logger.warn("Failed to unload provider %s: %s", safeId(internal), e.getMessage());
			fireProviderUnloaded(internal);
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

	private void registerConflictResolvers(IdenticaProvider provider) {
		if (provider == null) return;
		for (var resolver : provider.getConflictResolvers())
			conflictService.register(resolver);

		for (var type : provider.getConflictTypes())
			conflictService.register(type);
	}

	private void unregisterConflictResolvers(IdenticaProvider provider) {
		if (provider == null) return;
		for (var resolver : provider.getConflictResolvers())
			conflictService.unregister(resolver);

		for (var type : provider.getConflictTypes())
			conflictService.unregister(type);

	}

	private void fireProviderDisabled(InternalProvider internal) {
		if (eventManager == null || internal == null) return;
		eventManager.call(new ProviderDisabledEvent(internal));
	}

	private void fireProviderLoaded(InternalProvider internal) {
		if (eventManager == null || internal == null) return;
		eventManager.call(new ProviderLoadedEvent(internal));
	}

	private void fireProviderEnabled(InternalProvider internal) {
		if (eventManager == null || internal == null) return;
		eventManager.call(new ProviderEnabledEvent(internal));
	}

	private void fireProviderUnloaded(InternalProvider internal) {
		if (eventManager == null || internal == null) return;
		eventManager.call(new ProviderUnloadedEvent(internal));
	}

	private String safeId(InternalProvider internal) {
		if (internal == null || internal.getDescriptor() == null)
			return "unknown";

		String id = internal.getDescriptor().getId();
		return !id.isBlank() ? id : "unknown";
	}
}
