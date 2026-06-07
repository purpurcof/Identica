package me.whereareiam.identica.feature.verification.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import me.whereareiam.identica.command.CommandService;
import me.whereareiam.identica.feature.verification.command.suggestion.VerificationMethodSuggestions;

import java.util.Map;
import java.util.Set;

@Singleton
public class VerificationCommandRegistrar {
	private final CommandService commandService;
	private final VerificationMethodSuggestions verificationMethodSuggestions;
	private final Set<Object> commandInstances;

	@Inject
	public VerificationCommandRegistrar(
			CommandService commandService,
			VerificationMethodSuggestions verificationMethodSuggestions,
			@Named("verificationCommandInstances") Set<Object> commandInstances
	) {
		this.commandService = commandService;
		this.verificationMethodSuggestions = verificationMethodSuggestions;
		this.commandInstances = commandInstances;
	}

	public void registerCommands() {
		commandService.registerSuggestionProvider(VerificationMethodSuggestions.KEY, verificationMethodSuggestions);
		commandService.registerCommandInstances(Map.of(), commandInstances.toArray());
	}
}
