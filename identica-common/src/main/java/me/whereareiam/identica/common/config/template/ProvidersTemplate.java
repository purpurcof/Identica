package me.whereareiam.identica.common.config.template;

import com.google.inject.Singleton;
import me.whereareiam.configura.TemplateProvider;
import me.whereareiam.identica.model.config.Providers;

import java.util.List;

@Singleton
public class ProvidersTemplate implements TemplateProvider<Providers> {
	@Override
	public Providers supply(Providers config) {
		Providers.ConflictPolicy premiumWins = new Providers.ConflictPolicy();
		premiumWins.setWhen(List.of("Premium", "Cracked"));
		premiumWins.setKey("username");
		premiumWins.setActions(List.of("allow", "kick_existing", "rename_existing"));

		Providers.ConflictPolicy deny = new Providers.ConflictPolicy();
		deny.setWhen(List.of("Cracked", "Cracked"));
		deny.setKey("username");
		deny.setActions(List.of("deny"));

		Providers.Conflict conflicts = new Providers.Conflict();
		conflicts.setKeys(List.of("username"));
		conflicts.setDefaultActions(List.of("allow"));
		conflicts.setPolicies(List.of(premiumWins, deny));
		config.setConflicts(conflicts);

		Providers.ProviderEntry cracked = new Providers.ProviderEntry();
		cracked.setId("Cracked");
		cracked.setEnabled(true);
		cracked.setPriority(50);

		Providers.ProviderEntry premium = new Providers.ProviderEntry();
		premium.setId("Premium");
		premium.setEnabled(true);
		premium.setPriority(100);

		config.setProviders(List.of(cracked, premium));
		return config;
	}
}
