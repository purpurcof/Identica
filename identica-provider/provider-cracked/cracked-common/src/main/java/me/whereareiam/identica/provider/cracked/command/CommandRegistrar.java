package me.whereareiam.identica.provider.cracked.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import me.whereareiam.identica.command.CommandService;
import me.whereareiam.identica.provider.cracked.config.CrackedCommands;

import java.util.Set;

@Singleton
public class CommandRegistrar {
	private final CommandService commandService;
	private final CrackedCommands crackedCommands;
	private final Set<Object> commandInstances;

	@Inject
	public CommandRegistrar(
			CommandService commandService,
			@Named("cracked") CrackedCommands crackedCommands,
			@Named("crackedCommandInstances") Set<Object> commandInstances
	) {
		this.commandService = commandService;
		this.crackedCommands = crackedCommands;
		this.commandInstances = commandInstances;
	}

	public void registerCommands() {
		commandService.registerCommandInstances(crackedCommands.getCommands(), commandInstances.toArray());
	}
}
