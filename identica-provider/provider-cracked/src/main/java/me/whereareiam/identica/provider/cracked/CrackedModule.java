package me.whereareiam.identica.provider.cracked;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import me.whereareiam.identica.model.config.Commands;
import me.whereareiam.identica.provider.cracked.config.CrackedMessages;
import me.whereareiam.identica.provider.cracked.config.CrackedSettings;
import me.whereareiam.identica.provider.cracked.config.provider.CrackedCommandsProvider;
import me.whereareiam.identica.provider.cracked.config.provider.CrackedMessagesProvider;
import me.whereareiam.identica.provider.cracked.config.provider.CrackedSettingsProvider;

public class CrackedModule extends AbstractModule {
	@Override
	protected void configure() {
		bind(CrackedSettingsProvider.class).asEagerSingleton();
		bind(CrackedSettings.class).toProvider(CrackedSettingsProvider.class);

		bind(CrackedMessagesProvider.class).asEagerSingleton();
		bind(CrackedMessages.class).toProvider(CrackedMessagesProvider.class);

		bind(CrackedCommandsProvider.class).asEagerSingleton();
		bind(Commands.class)
				.annotatedWith(Names.named("cracked"))
				.toProvider(CrackedCommandsProvider.class);
	}
}
