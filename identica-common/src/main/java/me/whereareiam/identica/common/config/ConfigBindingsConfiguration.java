package me.whereareiam.identica.common.config;

import com.google.inject.AbstractModule;
import me.whereareiam.identica.common.config.provider.*;
import me.whereareiam.identica.model.config.Commands;
import me.whereareiam.identica.model.config.Messages;
import me.whereareiam.identica.model.config.Replication;
import me.whereareiam.identica.model.config.Settings;
import me.whereareiam.identica.model.config.Verification;
import me.whereareiam.identica.model.config.persistence.Persistence;
import me.whereareiam.identica.model.config.provider.Conflicts;
import me.whereareiam.identica.model.config.provider.Providers;

public class ConfigBindingsConfiguration extends AbstractModule {
	@Override
	protected void configure() {
		bind(Settings.class).toProvider(SettingsProvider.class);
		bind(Messages.class).toProvider(MessagesProvider.class);
		bind(Commands.class).toProvider(CommandsProvider.class);
		bind(Conflicts.class).toProvider(ConflictsProvider.class);
		bind(Providers.class).toProvider(ProvidersProvider.class);
		bind(Verification.class).toProvider(VerificationProvider.class);
		bind(Persistence.class).toProvider(PersistenceProvider.class);
		bind(Replication.class).toProvider(ReplicationProvider.class);
	}
}
