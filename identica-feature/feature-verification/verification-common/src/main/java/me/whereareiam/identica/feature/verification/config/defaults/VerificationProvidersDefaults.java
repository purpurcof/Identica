package me.whereareiam.identica.feature.verification.config.defaults;

import com.google.inject.Singleton;
import me.whereareiam.configura.merge.defaults.DefaultsProvider;
import me.whereareiam.identica.feature.verification.model.config.VerificationProviders;
import me.whereareiam.identica.feature.verification.type.UnavailableSelectionPolicy;
import me.whereareiam.identica.model.config.provider.Providers;

import java.util.List;

@Singleton
public class VerificationProvidersDefaults implements DefaultsProvider<Providers> {
	@Override
	public Providers supply(Providers config) {
		for (Providers.ProviderEntry entry : config.getProviders()) {
			if (entry == null || entry.getId().isBlank()) continue;
			if (!(entry.getFeatures() instanceof VerificationProviders features)) continue;

			if ("credential".equalsIgnoreCase(entry.getId()))
				features.setVerification(defaultVerification());
			if ("premium".equalsIgnoreCase(entry.getId()))
				features.setVerification(defaultVerification());
		}

		return config;
	}

	private VerificationProviders.Verification defaultVerification() {
		VerificationProviders.Verification verification = new VerificationProviders.Verification();
		verification.setEnabled(true);
		verification.setRequired(false);
		verification.setUnavailableSelectionPolicy(UnavailableSelectionPolicy.KEEP_LOCKED);

		VerificationProviders.Verification.MethodEntry totp = new VerificationProviders.Verification.MethodEntry();
		totp.setId("totp");
		totp.setEnabled(true);
		totp.setPriority(100);
		totp.setUnavailableSelectionPolicy(UnavailableSelectionPolicy.KEEP_LOCKED);
		verification.setMethods(List.of(totp));
		return verification;
	}
}
