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
		premiumWins.setPriority("Premium");

		Providers.Resolution premiumResolution = new Providers.Resolution();
		Providers.Loser premiumLoser = new Providers.Loser();
		premiumLoser.setProvider("Cracked");
		premiumLoser.setSolution("username_prefix");
		premiumResolution.setLoser(premiumLoser);

		Providers.Winner premiumWinner = new Providers.Winner();
		premiumWinner.setAction("kick_existing");
		premiumResolution.setWinner(premiumWinner);

		premiumWins.setResolution(premiumResolution);

		Providers.ConflictPolicy deny = new Providers.ConflictPolicy();
		deny.setWhen(List.of("Cracked", "Cracked"));
		deny.setKey("username");

		Providers.Resolution denyResolution = new Providers.Resolution();
		Providers.NewConnection denyNew = new Providers.NewConnection();
		denyNew.setAction("deny");
		denyNew.setMessage("Username already in use by another cracked account");
		denyResolution.setNewConnection(denyNew);
		deny.setResolution(denyResolution);

		Providers.Conflicts conflicts = new Providers.Conflicts();
		conflicts.setKeys(List.of("username"));
		conflicts.setDefaultPolicy("proceed");
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
