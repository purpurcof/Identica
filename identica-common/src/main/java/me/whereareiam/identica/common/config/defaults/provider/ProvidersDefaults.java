package me.whereareiam.identica.common.config.defaults.provider;

import com.google.inject.Singleton;
import me.whereareiam.configura.merge.defaults.DefaultsProvider;
import me.whereareiam.identica.model.config.provider.Providers;

import java.time.Duration;
import java.util.List;

@Singleton
public class ProvidersDefaults implements DefaultsProvider<Providers> {
	@Override
	public Providers supply(Providers config) {
		Providers.Behavior behavior = new Providers.Behavior();
		behavior.setAttemptTtl(Duration.ofMinutes(10));
		config.setBehavior(behavior);

		Providers.ProviderEntry credential = new Providers.ProviderEntry();
		credential.setId("credential");
		credential.setDisplayName("Credential");
		credential.setEnabled(true);
		credential.setPriority(50);
		credential.setEntrypoints(List.of("credential.arcadeya.com"));

		Providers.ProviderEntry premium = new Providers.ProviderEntry();
		premium.setId("premium");
		premium.setDisplayName("Premium");
		premium.setEnabled(true);
		premium.setPriority(100);
		premium.setEntrypoints(List.of("premium.arcadeya.com"));

		config.setProviders(List.of(credential, premium));
		return config;
	}
}
