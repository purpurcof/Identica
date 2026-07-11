package me.whereareiam.identica.provider.premium.config.defaults;

import com.google.inject.Singleton;
import me.whereareiam.configura.merge.defaults.DefaultsProvider;
import me.whereareiam.identica.provider.premium.config.PremiumSettings;

import java.time.Duration;

@Singleton
public class PremiumSettingsDefaults implements DefaultsProvider<PremiumSettings> {
	@Override
	public PremiumSettings supply(PremiumSettings config) {
		PremiumSettings.Lookup lookup = config.getLookup();
		lookup.setProfileEndpoint("https://api.mojang.com/users/profiles/minecraft/%s");
		lookup.setTimeout(Duration.ofSeconds(3));
		lookup.setCacheTtl(Duration.ofMinutes(5));

		config.setLookup(lookup);
		config.setProfileSnapshotTtl(Duration.ofMinutes(10));

		PremiumSettings.Cache cache = new PremiumSettings.Cache();
		cache.setProfile("premium-resolver");
		cache.setProfileSnapshot("premium-profile-snapshot");
		PremiumSettings.Replication replication = new PremiumSettings.Replication();
		replication.setCache(cache);
		config.setReplication(replication);

		return config;
	}
}
