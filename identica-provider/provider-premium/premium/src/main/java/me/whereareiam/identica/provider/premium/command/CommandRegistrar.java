package me.whereareiam.identica.provider.premium.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import me.whereareiam.identica.command.CommandService;
import me.whereareiam.identica.model.config.Commands;

import java.util.Set;

@Singleton
public class CommandRegistrar {
	private final CommandService commandService;
	private final Commands premiumCommands;
	private final Set<Object> commandInstances;

	@Inject
	public CommandRegistrar(
			CommandService commandService,
			@Named("premium") Commands premiumCommands,
			@Named("premiumCommandInstances") Set<Object> commandInstances
	) {
		this.commandService = commandService;
		this.premiumCommands = premiumCommands;
		this.commandInstances = commandInstances;
	}

	public void registerCommands() {
		commandService.registerCommandInstances(premiumCommands.getCommands(), commandInstances.toArray());
	}
}
