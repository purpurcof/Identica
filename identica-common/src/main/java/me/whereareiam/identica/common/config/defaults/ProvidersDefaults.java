package me.whereareiam.identica.common.config.defaults;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.Singleton;
import me.whereareiam.configura.merge.defaults.MergeDefaultsProvider;
import me.whereareiam.identica.model.config.Providers;
import me.whereareiam.identica.type.provider.ProviderJoinRestrictionCondition;
import me.whereareiam.identica.type.verification.UnavailableSelectionPolicy;

import java.util.List;

@Singleton
public class ProvidersDefaults implements MergeDefaultsProvider<Providers> {
	@Override
	public Providers supply(Providers config) {
		Providers.ConflictRules usernameRules = new Providers.ConflictRules();
		Providers.ConflictRules.ConflictRule defaultRule = new Providers.ConflictRules.ConflictRule();
		defaultRule.setResolvers(List.of(formatResolver("{username}*")));
		usernameRules.setDefaultRule(defaultRule);

		Providers.ConflictRules.ConflictRule premiumVsCredential = new Providers.ConflictRules.ConflictRule();
		premiumVsCredential.setProviders(List.of("premium", "credential"));
		premiumVsCredential.setResolvers(List.of(formatResolver("{username}_{incomingProvider}")));
		usernameRules.setPairs(List.of(premiumVsCredential));

		config.getConflicts().put("username", usernameRules);

		Providers.ProviderEntry credential = new Providers.ProviderEntry();
		credential.setId("credential");
		credential.setDisplayName("CR");
		credential.setEnabled(true);
		credential.setPriority(50);
		credential.setEntrypoints(List.of("credential.arcadeya.com"));
		credential.setJoinRestriction(joinRestriction(
				ProviderJoinRestrictionCondition.RECOGNIZED,
				ProviderJoinRestrictionCondition.LINKED
		));
		credential.setVerification(credentialVerification());

		Providers.ProviderEntry premium = new Providers.ProviderEntry();
		premium.setId("premium");
		premium.setDisplayName("PR");
		premium.setEnabled(true);
		premium.setPriority(100);
		premium.setEntrypoints(List.of("premium.arcadeya.com"));
		premium.setJoinRestriction(joinRestriction(ProviderJoinRestrictionCondition.RECOGNIZED));
		premium.setVerification(premiumVerification());

		config.setProviders(List.of(credential, premium));
		return config;
	}

	private Providers.ConflictRules.ConflictRule.ResolverEntry formatResolver(String pattern) {
		ObjectNode format = JsonNodeFactory.instance.objectNode();
		format.put("pattern", pattern);

		ObjectNode node = JsonNodeFactory.instance.objectNode();
		node.set("format", format);
		node.put("target", "joiner");
		Providers.ConflictRules.ConflictRule.ResolverEntry entry =
				new Providers.ConflictRules.ConflictRule.ResolverEntry();
		entry.setId("format_display");
		entry.setParameters(node);
		return entry;
	}

	private Providers.ProviderEntry.Verification credentialVerification() {
		Providers.ProviderEntry.Verification verification = new Providers.ProviderEntry.Verification();
		verification.setEnabled(true);
		verification.setRequired(false);
		verification.setUnavailableSelectionPolicy(UnavailableSelectionPolicy.KEEP_LOCKED);

		Providers.ProviderEntry.Verification.MethodEntry totp =
				new Providers.ProviderEntry.Verification.MethodEntry();
		totp.setId("totp");
		totp.setEnabled(true);
		totp.setPriority(100);
		totp.setUnavailableSelectionPolicy(UnavailableSelectionPolicy.KEEP_LOCKED);
		verification.setMethods(List.of(totp));

		return verification;
	}

	private Providers.ProviderEntry.Verification premiumVerification() {
		Providers.ProviderEntry.Verification verification = new Providers.ProviderEntry.Verification();
		verification.setEnabled(true);
		verification.setRequired(false);
		verification.setUnavailableSelectionPolicy(UnavailableSelectionPolicy.KEEP_LOCKED);

		Providers.ProviderEntry.Verification.MethodEntry totp =
				new Providers.ProviderEntry.Verification.MethodEntry();
		totp.setId("totp");
		totp.setEnabled(true);
		totp.setPriority(100);
		totp.setUnavailableSelectionPolicy(UnavailableSelectionPolicy.KEEP_LOCKED);
		verification.setMethods(List.of(totp));

		return verification;
	}

	private Providers.ProviderEntry.JoinRestriction joinRestriction(ProviderJoinRestrictionCondition... allow) {
		Providers.ProviderEntry.JoinRestriction restriction = new Providers.ProviderEntry.JoinRestriction();
		restriction.setEnabled(false);
		restriction.setAllow(List.of(allow));
		return restriction;
	}
}
