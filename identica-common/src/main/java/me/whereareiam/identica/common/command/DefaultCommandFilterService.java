package me.whereareiam.identica.common.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.whereareiam.identica.command.CommandDefinitionCollector;
import me.whereareiam.identica.command.CommandFilterService;
import me.whereareiam.identica.command.CommandService;
import me.whereareiam.identica.identity.actor.Identity;
import me.whereareiam.identica.identity.session.SessionService;
import me.whereareiam.keystone.Actor;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Singleton
public class DefaultCommandFilterService implements CommandFilterService {
	private final SessionService sessionService;
	private final CommandDefinitionCollector definitionCollector;
	private final CommandService commandService;

	@Inject
	public DefaultCommandFilterService(
			SessionService sessionService,
			CommandDefinitionCollector definitionCollector,
			CommandService commandService
	) {
		this.sessionService = sessionService;
		this.definitionCollector = definitionCollector;
		this.commandService = commandService;
	}

	@Override
	public boolean isAllowed(@NotNull Actor sender, @NotNull String commandLine) {
		if (!(sender instanceof Identity identity))
			return true;

		UUID accountUniqueId = identity.getAccountUniqueId();
		if (accountUniqueId != null) {
			boolean hasSession = sessionService.findByUniqueId(accountUniqueId)
					.join()
					.isPresent();
			if (hasSession)
				return true;
		}

		String normalized = normalize(commandLine);
		if (normalized == null || normalized.isBlank())
			return false;

		Set<String> allowedAliases = definitionCollector.getAllowedDuringAuthAliases();
		String firstWord = firstWord(normalized);

		if (firstWord == null)
			return allowedAliases.contains(normalized);

		if (!allowedAliases.contains(firstWord))
			return false;

		String rest = normalized.substring(firstWord.length()).trim();
		if (rest.isEmpty())
			return true;

		String entire = firstWord + " " + rest;
		if (allowedAliases.contains(entire))
			return true;

		return !isKnownSubcommand(rest, allowedAliases);
	}

	private boolean isKnownSubcommand(String rest, Set<String> allowedAliases) {
		Set<String> allAliases = new HashSet<>();
		commandService.getRegisteredDefinitions().values().forEach(def -> {
			if (def.getAliases() != null)
				allAliases.addAll(def.getAliases());
		});

		for (String alias : allAliases) {
			if (allowedAliases.contains(alias))
				continue;
			if (rest.equals(alias) || rest.startsWith(alias + " "))
				return true;
		}
		return false;
	}

	private String normalize(String commandLine) {
		if (commandLine == null || commandLine.isBlank())
			return null;

		String trimmed = commandLine.trim();
		if (trimmed.startsWith("/"))
			trimmed = trimmed.substring(1);

		return trimmed;
	}

	private String firstWord(String normalized) {
		int spaceIndex = normalized.indexOf(' ');
		return spaceIndex > 0 ? normalized.substring(0, spaceIndex) : null;
	}
}
