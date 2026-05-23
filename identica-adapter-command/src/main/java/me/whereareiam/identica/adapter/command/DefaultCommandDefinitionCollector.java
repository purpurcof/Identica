package me.whereareiam.identica.adapter.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.whereareiam.identica.command.CommandDefinitionCollector;
import me.whereareiam.identica.command.CommandService;
import me.whereareiam.identica.model.CommandDefinition;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Singleton
public class DefaultCommandDefinitionCollector implements CommandDefinitionCollector {
	private final CommandService commandService;
	private volatile Set<String> cachedAliases = null;

	@Inject
	public DefaultCommandDefinitionCollector(CommandService commandService) {
		this.commandService = commandService;
	}

	@Override
	public @NotNull Set<String> getAllowedDuringAuthAliases() {
		Set<String> local = cachedAliases;
		if (local != null)
			return local;

		synchronized (this) {
			local = cachedAliases;
			if (local != null)
				return local;

			Map<String, CommandDefinition> definitions = commandService.getRegisteredDefinitions();
			Set<String> aliases = new HashSet<>();
			for (Map.Entry<String, CommandDefinition> entry : definitions.entrySet()) {
				CommandDefinition def = entry.getValue();
				if (def.getAliases() == null)
					continue;

				if (def.isAllowedDuringAuth() || "main".equals(entry.getKey()))
					aliases.addAll(def.getAliases());
			}

			cachedAliases = Collections.unmodifiableSet(aliases);
			return cachedAliases;
		}
	}

	public void invalidateCache() {
		cachedAliases = null;
	}
}
