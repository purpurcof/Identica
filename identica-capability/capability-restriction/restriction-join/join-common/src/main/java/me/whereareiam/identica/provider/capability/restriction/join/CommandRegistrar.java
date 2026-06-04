package me.whereareiam.identica.provider.capability.restriction.join;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import me.whereareiam.identica.command.CommandService;
import me.whereareiam.identica.provider.capability.restriction.join.config.JoinRestrictionCommands;

import java.util.Set;

@Singleton
public class CommandRegistrar {
	private final CommandService commandService;
	private final JoinRestrictionCommands commands;
	private final Set<Object> commandInstances;

	@Inject
	public CommandRegistrar(
			CommandService commandService,
			@Named("joinRestriction") JoinRestrictionCommands commands,
			@Named("joinRestrictionCommandInstances") Set<Object> commandInstances
	) {
		this.commandService = commandService;
		this.commands = commands;
		this.commandInstances = commandInstances;
	}

	public void registerCommands() {
		commandService.registerCommandInstances(commands.getCommands(), commandInstances.toArray());
	}
}
