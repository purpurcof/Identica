package me.whereareiam.identica.common.config.defaults.provider;

import com.google.inject.Singleton;
import me.whereareiam.configura.merge.defaults.DefaultsProvider;
import me.whereareiam.identica.model.config.provider.Providers;
import me.whereareiam.identica.type.verification.UnavailableSelectionPolicy;

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
		credential.setVerification(credentialVerification());

		Providers.ProviderEntry premium = new Providers.ProviderEntry();
		premium.setId("premium");
		premium.setDisplayName("Premium");
		premium.setEnabled(true);
		premium.setPriority(100);
		premium.setEntrypoints(List.of("premium.arcadeya.com"));
		premium.setVerification(premiumVerification());

		config.setProviders(List.of(credential, premium));
		return config;
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
}
