package me.whereareiam.identica.common.loader;

import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.loader.ProviderManager;
import me.whereareiam.identica.model.provider.InternalProvider;
import me.whereareiam.identica.model.config.Providers;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import me.whereareiam.identica.type.ProviderState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class DefaultProviderManager implements ProviderManager {
	private final ProviderDiscovery discovery;
	private final ProviderLifecycleController lifecycleController;
	private final Provider<Providers> providersConfig;

	private final List<InternalProvider> providers = new ArrayList<>();

	@Override
	public void loadProviders() {
		providers.clear();
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

	private List<InternalProvider> selectEnabled(List<InternalProvider> discovered, Providers config) {
		if (config == null || config.getProviders() == null || config.getProviders().isEmpty()) {
			return discovered.stream()
					.peek(provider -> provider.setPriority(provider.getDescriptor().getPriorityDefault()))
					.sorted(Comparator.comparingInt(InternalProvider::getPriority).reversed())
					.collect(Collectors.toList());
		}

		Map<String, Providers.ProviderEntry> entries = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		for (Providers.ProviderEntry entry : config.getProviders()) {
			if (entry == null || entry.getId() == null || entry.getId().isBlank()) {
				continue;
			}
			entries.putIfAbsent(entry.getId().trim(), entry);
		}

		return discovered.stream()
				.filter(provider -> {
					Providers.ProviderEntry entry = entries.get(provider.getDescriptor().getId());
					return entry != null && entry.isEnabled();
				})
				.peek(provider -> {
					Providers.ProviderEntry entry = entries.get(provider.getDescriptor().getId());
					int priority = entry != null ? entry.getPriority() : provider.getDescriptor().getPriorityDefault();
					provider.setPriority(priority);
				})
				.sorted(Comparator.comparingInt(InternalProvider::getPriority).reversed())
				.collect(Collectors.toList());
	}
}
