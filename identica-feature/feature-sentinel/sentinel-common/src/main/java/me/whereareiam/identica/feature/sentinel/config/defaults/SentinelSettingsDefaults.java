package me.whereareiam.identica.feature.sentinel.config.defaults;

import com.google.inject.Singleton;
import me.whereareiam.configura.merge.defaults.DefaultsProvider;
import me.whereareiam.identica.feature.sentinel.model.config.SentinelSettings;
import me.whereareiam.identica.model.sentinel.SentinelPolicy;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

@Singleton
public class SentinelSettingsDefaults implements DefaultsProvider<SentinelSettings> {
	@Override
	public SentinelSettings supply(@NotNull SentinelSettings settings) {
		settings.setSentinels(defaultSentinels());
		settings.setCache(defaultCache());
		return settings;
	}

	private SentinelSettings.Sentinels defaultSentinels() {
		SentinelSettings.Sentinels sentinels = new SentinelSettings.Sentinels();
		SentinelPolicy resumeSpam = new SentinelPolicy();
		resumeSpam.setEnabled(false);
		resumeSpam.setMaxAttempts(10);

		SentinelPolicy.Lockout lockout = new SentinelPolicy.Lockout();
		lockout.setEnabled(true);
		lockout.setDuration(Duration.ofSeconds(30));
		resumeSpam.setLockout(lockout);

		SentinelPolicy.Warning warning = new SentinelPolicy.Warning();
		warning.setEnabled(false);
		warning.setThresholdPercentage(0);
		resumeSpam.setWarning(warning);

		sentinels.setResumeSpam(resumeSpam);
		return sentinels;
	}

	private SentinelSettings.Cache defaultCache() {
		SentinelSettings.Cache cache = new SentinelSettings.Cache();
		cache.setSentinels("identica:sentinels");
		return cache;
	}
}
