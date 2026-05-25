package me.whereareiam.identica.common.sentinel;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import me.whereareiam.identica.Registry;
import me.whereareiam.identica.sentinel.SentinelDefinition;
import me.whereareiam.identica.sentinel.SentinelService;

public class SentinelConfiguration extends AbstractModule {
	@Override
	protected void configure() {
		bind(new TypeLiteral<Registry<SentinelDefinition>>() {})
				.to(SentinelRegistry.class)
				.asEagerSingleton();
		bind(SentinelService.class).to(DefaultSentinelService.class).asEagerSingleton();
	}
}
