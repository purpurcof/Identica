package me.whereareiam.identica.common.config.template;

import com.google.inject.Singleton;
import me.whereareiam.configura.TemplateProvider;
import me.whereareiam.configura.node.ObjectNode;
import me.whereareiam.configura.node.StringNode;
import me.whereareiam.identica.model.config.Providers;

import java.util.List;

@Singleton
public class ProvidersTemplate implements TemplateProvider<Providers> {
	@Override
	public Providers supply(Providers config) {
		Providers.ConflictRules usernameRules = new Providers.ConflictRules();
		Providers.ConflictRule defaultRule = new Providers.ConflictRule();
		defaultRule.setResolvers(List.of(formatResolver("{username}*", "joiner")));
		usernameRules.setDefaultRule(defaultRule);

		Providers.ConflictRule premiumVsCracked = new Providers.ConflictRule();
		premiumVsCracked.setProviders(List.of("premium", "cracked"));
		premiumVsCracked.setResolvers(List.of(formatResolver("{username}_{incomingProvider}", "joiner")));
		usernameRules.setPairs(List.of(premiumVsCracked));

		config.getConflicts().put("username", usernameRules);

		Providers.ProviderEntry cracked = new Providers.ProviderEntry();
		cracked.setId("cracked");
		cracked.setDisplayName("CR");
		cracked.setEnabled(true);
		cracked.setPriority(50);
		cracked.setEntrypoints(List.of("cracked.arcadeya.com"));

		Providers.ProviderEntry premium = new Providers.ProviderEntry();
		premium.setId("premium");
		premium.setDisplayName("PR");
		premium.setEnabled(true);
		premium.setPriority(100);
		premium.setEntrypoints(List.of("premium.arcadeya.com"));

		config.setProviders(List.of(cracked, premium));
		return config;
	}

	private Providers.ResolverEntry formatResolver(String pattern, String target) {
		ObjectNode format = new ObjectNode();
		format.getValues().put("pattern", new StringNode(pattern));

		ObjectNode node = new ObjectNode();
		node.getValues().put("format", format);
		node.getValues().put("target", new StringNode(target));
		Providers.ResolverEntry entry = new Providers.ResolverEntry();
		entry.setId("format_display");
		entry.setParameters(node);
		return entry;
	}
}
