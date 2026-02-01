package me.whereareiam.identica.common.provider;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import me.whereareiam.identica.common.provider.resolver.ProviderPlatformResolver;
import me.whereareiam.identica.common.provider.resolver.ProviderResolverRegistry;
import me.whereareiam.identica.logging.Logger;
import me.whereareiam.identica.model.config.Providers;
import me.whereareiam.identica.model.provider.InternalProvider;
import me.whereareiam.identica.model.provider.ProviderDescriptor;
import me.whereareiam.identica.provider.ProviderManager;
import me.whereareiam.identica.provider.profile.ProfileResolution;
import me.whereareiam.identica.provider.profile.ProfileResolveContext;
import me.whereareiam.identica.provider.profile.ProfileSubjectResolver;
import me.whereareiam.identica.provider.resolver.ProviderResolver;
import me.whereareiam.identica.type.provider.ProviderCapability;
import me.whereareiam.identica.type.provider.ProviderState;
import me.whereareiam.identica.util.UniqueIdGenerator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

@Singleton
public class DefaultProviderManager implements ProviderManager {
	private final ProviderDiscovery discovery;
	private final ProviderLifecycleController lifecycleController;
	private final Provider<Providers> providersConfig;
	private final ProviderResolverRegistry resolverRegistry;

	private final List<InternalProvider> providers = new ArrayList<>();

	@Inject
	public DefaultProviderManager(
			ProviderDiscovery discovery,
			ProviderLifecycleController lifecycleController,
			Provider<Providers> providersConfig,
			ProviderResolverRegistry resolverRegistry,
			ProviderPlatformResolver platformResolver
	) {
		this.discovery = discovery;
		this.lifecycleController = lifecycleController;
		this.providersConfig = providersConfig;
		this.resolverRegistry = resolverRegistry;

		registerResolver(platformResolver);
	}

	@Override
	public void loadProviders() {
		providers.clear();
		Logger.debug("Discovering providers...");
		List<InternalProvider> discovered = discovery.discover(providers);
		List<InternalProvider> enabledProviders = selectEnabled(discovered, providersConfig.get());

		for (InternalProvider provider : enabledProviders) {
			lifecycleController.loadProvider(provider);
			lifecycleController.enableProvider(provider);
			if (provider.getState() == ProviderState.ENABLED) {
				providers.add(provider);
			}
		}
	}

	@Override
	public void unloadProviders() {
		for (InternalProvider provider : providers) {
			lifecycleController.disableProvider(provider);
		}
		for (InternalProvider provider : providers) {
			lifecycleController.unloadProvider(provider);
		}
	}

	@Override
	public List<InternalProvider> getProviders() {
		return Collections.unmodifiableList(providers);
	}

	@Override
	public @Nullable ProfileResolution resolveProfile(@NotNull ProfileResolveContext context) {
		if (providers.isEmpty()) return null;

		List<InternalProvider> sorted = new ArrayList<>(providers);
		sorted.sort(Comparator.comparingInt(InternalProvider::getPriority)
				.reversed()
				.thenComparing(left -> left.getDescriptor().getId(), String.CASE_INSENSITIVE_ORDER));

		for (InternalProvider provider : sorted) {
			if (provider == null || provider.getState() != ProviderState.ENABLED) continue;
			Set<ProfileSubjectResolver> registered = provider.getProfileSubjectResolvers();
			if (registered == null || registered.isEmpty()) continue;

			List<ProfileSubjectResolver> ordered = new ArrayList<>(registered);
			ordered.sort(Comparator.comparingInt(ProfileSubjectResolver::priority).reversed());

			for (ProfileSubjectResolver resolver : ordered) {
				if (resolver == null || !resolver.supports(context))
					continue;

				ProfileResolution resolution = resolver.resolve(context);
				if (resolution == null)
					continue;

				String providerId = resolution.getProviderId();
				String providerSubject = resolution.getProviderSubject();
				if (providerId.isBlank() || providerSubject.isBlank()) continue;

				return resolution;
			}
		}

		return resolveOfflineProfileFallback(context);
	}

	@Override
	public @NotNull List<InternalProvider> findProviders(ProviderCapability... capabilities) {
		if (providers.isEmpty()) return List.of();

		List<InternalProvider> matches = new ArrayList<>();
		for (InternalProvider provider : providers) {
			if (provider == null || provider.getState() != ProviderState.ENABLED) continue;

			ProviderDescriptor descriptor = provider.getDescriptor();
			if (descriptor == null) continue;

			String id = descriptor.getId();
			if (id.isBlank()) continue;
			if (!supportsAll(descriptor, capabilities)) continue;

			matches.add(provider);
		}

		matches.sort(Comparator.comparingInt(InternalProvider::getPriority)
				.reversed()
				.thenComparing(left -> left.getDescriptor().getId(), String.CASE_INSENSITIVE_ORDER));

		return Collections.unmodifiableList(matches);
	}

	@Override
	public InternalProvider findProvider(ProviderCapability... capabilities) {
		List<InternalProvider> matches = findProviders(capabilities);
		if (matches.isEmpty()) return null;

		return matches.getFirst();
	}

	@Override
	public void registerResolver(ProviderResolver resolver) {
		resolverRegistry.register(resolver);
	}

	@Override
	public void unregisterResolver(ProviderResolver resolver) {
		resolverRegistry.unregister(resolver);
	}

	@Override
	public List<ProviderResolver> getResolvers() {
		return resolverRegistry.getAll();
	}

	private List<InternalProvider> selectEnabled(List<InternalProvider> discovered, Providers config) {
		if (config == null || config.getProviders().isEmpty()) {
			return discovered.stream()
					.peek(provider -> provider.setPriority(provider.getDescriptor().getPriority()))
					.sorted(Comparator.comparingInt(InternalProvider::getPriority).reversed())
					.collect(Collectors.toList());
		}

		Map<String, Providers.ProviderEntry> entries = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		for (Providers.ProviderEntry entry : config.getProviders()) {
			if (entry == null || entry.getId().isBlank())
				continue;

			entries.putIfAbsent(entry.getId().trim(), entry);
		}

		return discovered.stream()
				.filter(provider -> {
					Providers.ProviderEntry entry = entries.get(provider.getDescriptor().getId());
					return entry != null && entry.isEnabled();
				})
				.peek(provider -> {
					Providers.ProviderEntry entry = entries.get(provider.getDescriptor().getId());
					int priority = entry != null ? entry.getPriority() : provider.getDescriptor().getPriority();
					provider.setPriority(priority);
				})
				.sorted(Comparator.comparingInt(InternalProvider::getPriority).reversed())
				.collect(Collectors.toList());
	}

	private boolean supportsAll(ProviderDescriptor descriptor, ProviderCapability[] capabilities) {
		if (capabilities == null) return true;

		for (ProviderCapability capability : capabilities) {
			if (capability == null) continue;
			if (!descriptor.hasCapability(capability))
				return false;
		}

		return true;
	}

	private @Nullable ProfileResolution resolveOfflineProfileFallback(@NotNull ProfileResolveContext context) {
		InternalProvider provider = findProvider(ProviderCapability.OFFLINE_MODE);
		if (provider == null || provider.getDescriptor() == null)
			return null;

		String username = context.getUsername();
		if (username == null || username.isBlank())
			return null;

		UUID offlineUuid = UniqueIdGenerator.offlinePlayerUniqueId(username);
		if (offlineUuid == null)
			return null;

		return ProfileResolution.builder()
				.providerId(provider.getDescriptor().getId())
				.providerSubject(offlineUuid.toString())
				.build();
	}
}
