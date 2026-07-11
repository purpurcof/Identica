package me.whereareiam.identica.feature.sentinel;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import java.nio.file.Path;

public class SentinelFeatureConfiguration extends AbstractModule {
	@Override
	protected void configure() {
		install(new SentinelCommonConfiguration());
	}

	@Provides
	@Singleton
	@Named("sentinelFeaturePath")
	Path provideSentinelFeaturePath(@Named("featuresPath") Path featuresPath) {
		return featuresPath.resolve("sentinel");
	}
}
