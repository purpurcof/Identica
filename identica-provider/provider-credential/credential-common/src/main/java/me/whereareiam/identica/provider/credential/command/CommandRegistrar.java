package me.whereareiam.identica.provider.credential.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import me.whereareiam.identica.command.CommandService;
import me.whereareiam.identica.provider.credential.config.CredentialCommands;

import java.util.Set;

@Singleton
public class CommandRegistrar {
	private final CommandService commandService;
	private final CredentialCommands credentialCommands;
	private final Set<Object> commandInstances;

	@Inject
	public CommandRegistrar(
			CommandService commandService,
			@Named("credential") CredentialCommands credentialCommands,
			@Named("credentialCommandInstances") Set<Object> commandInstances
	) {
		this.commandService = commandService;
		this.credentialCommands = credentialCommands;
		this.commandInstances = commandInstances;
	}

	public void registerCommands() {
		commandService.registerCommandInstances(credentialCommands.getCommands(), commandInstances.toArray());
	}
}
