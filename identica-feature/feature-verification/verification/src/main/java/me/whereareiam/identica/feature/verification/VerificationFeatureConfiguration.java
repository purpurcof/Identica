package me.whereareiam.identica.feature.verification;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import me.whereareiam.identica.feature.verification.database.VerificationDatabaseConfiguration;

import java.nio.file.Path;

public class VerificationFeatureConfiguration extends AbstractModule {
	@Override
	protected void configure() {
		install(new VerificationCommonConfiguration());
		install(new VerificationDatabaseConfiguration());
	}

	@Provides
	@Singleton
	@Named("verificationFeaturePath")
	Path provideVerificationFeaturePath(@Named("featuresPath") Path featuresPath) {
		return featuresPath.resolve("verification");
	}
}
