package me.whereareiam.identica.feature.sentinel;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import me.whereareiam.identica.Registry;
import me.whereareiam.identica.feature.sentinel.config.provider.SentinelMessagesProvider;
import me.whereareiam.identica.feature.sentinel.config.provider.SentinelSettingsProvider;
import me.whereareiam.identica.feature.sentinel.model.config.SentinelMessages;
import me.whereareiam.identica.feature.sentinel.model.config.SentinelSettings;
import me.whereareiam.identica.feature.sentinel.type.ResumeSpamSentinelDefinition;
import me.whereareiam.identica.sentinel.SentinelDefinition;
import me.whereareiam.identica.sentinel.SentinelService;

public class SentinelCommonConfiguration extends AbstractModule {
	@Override
	protected void configure() {
		bind(SentinelMessages.class).toProvider(SentinelMessagesProvider.class);
		bind(SentinelSettings.class).toProvider(SentinelSettingsProvider.class);
		bind(new TypeLiteral<Registry<SentinelDefinition>>() {})
				.to(SentinelRegistry.class)
				.asEagerSingleton();
		bind(SentinelService.class).to(DefaultSentinelService.class).asEagerSingleton();
		bind(ConnectionAttemptSentinelLifecycle.class).asEagerSingleton();
		bind(ResumeSpamSentinelDefinition.class).asEagerSingleton();
	}
}
