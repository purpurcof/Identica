package me.whereareiam.identica.common.provider;

import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.model.config.Providers;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ProviderManager {
	private final ProviderDiscovery discovery;
	private final ProviderSelector selector;
	private final ProviderLoader loader;
	private final ProviderLifecycle lifecycle;
	private final Provider<Providers> providersConfig;

	private final List<InternalProvider> providers = new ArrayList<>();

	public List<InternalProvider> getProviders() {
		return Collections.unmodifiableList(providers);
	}

	public void loadProviders() {
		providers.clear();
		List<InternalProvider> discovered = discovery.discover(providers);
		List<InternalProvider> enabledProviders = selector.selectEnabled(discovered, providersConfig.get());

		for (InternalProvider provider : enabledProviders) {
			if (loader.load(provider)) {
				providers.add(provider);
			}
		}
	}

	public void unloadProviders() {
		for (InternalProvider provider : providers) {
			lifecycle.disable(provider);
			lifecycle.unload(provider);
		}
	}
}
