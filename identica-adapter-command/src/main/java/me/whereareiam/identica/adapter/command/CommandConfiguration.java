package me.whereareiam.identica.adapter.command;

import com.google.inject.AbstractModule;
import me.whereareiam.identica.command.CommandDefinitionCollector;
import me.whereareiam.identica.command.CommandService;

public class CommandConfiguration extends AbstractModule {
	@Override
	protected void configure() {
		bind(CommandService.class).to(DefaultCommandService.class).asEagerSingleton();
		bind(CommandDefinitionCollector.class).to(DefaultCommandDefinitionCollector.class).asEagerSingleton();
	}
}
