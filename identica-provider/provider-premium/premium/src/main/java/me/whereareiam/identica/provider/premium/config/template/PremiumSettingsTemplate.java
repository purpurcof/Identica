package me.whereareiam.identica.provider.premium.config.template;

import com.google.inject.Singleton;
import me.whereareiam.configura.TemplateProvider;
import me.whereareiam.identica.provider.premium.config.PremiumSettings;
import me.whereareiam.identica.provider.premium.type.VerificationFlow;

@Singleton
public class PremiumSettingsTemplate implements TemplateProvider<PremiumSettings> {
	@Override
	public PremiumSettings supply(PremiumSettings config) {
		PremiumSettings.Verification verification = config.getVerification();
		verification.setIntent(VerificationFlow.SILENT);

		PremiumSettings.Lookup lookup = config.getLookup();
		lookup.setProfileEndpoint("https://api.mojang.com/users/profiles/minecraft/%s");
		lookup.setTimeoutMs(3000);
		lookup.setCacheTtlMs(300000);

		config.setVerification(verification);
		config.setLookup(lookup);
		return config;
	}
}
