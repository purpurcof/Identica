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
		defaultRule.setResolver("format_display");
		defaultRule.setParameters(formatParams("{username}*"));
		usernameRules.setDefaultRule(defaultRule);

		Providers.ConflictRule premiumVsCracked = new Providers.ConflictRule();
		premiumVsCracked.setProviders(List.of("premium", "cracked"));
		premiumVsCracked.setResolver("format_display");
		premiumVsCracked.setParameters(formatParams("{username} [{incomingProvider}]#{random:2}"));
		usernameRules.setPairs(List.of(premiumVsCracked));

		config.getConflicts().put("username", usernameRules);

		Providers.ProviderEntry cracked = new Providers.ProviderEntry();
		cracked.setId("cracked");
		cracked.setEnabled(true);
		cracked.setPriority(50);

		Providers.ProviderEntry premium = new Providers.ProviderEntry();
		premium.setId("premium");
		premium.setEnabled(true);
		premium.setPriority(100);

		config.setProviders(List.of(cracked, premium));
		return config;
	}

	private ObjectNode formatParams(String pattern) {
		ObjectNode format = new ObjectNode();
		format.getValues().put("pattern", new StringNode(pattern));

		ObjectNode node = new ObjectNode();
		node.getValues().put("format", format);
		node.getValues().put("target", new StringNode("joiner"));
		return node;
	}
}
