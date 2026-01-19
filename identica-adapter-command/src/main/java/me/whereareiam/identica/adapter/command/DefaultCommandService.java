package me.whereareiam.identica.adapter.command;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import me.whereareiam.commandant.Commandant;
import me.whereareiam.commandant.CommandantKeys;
import me.whereareiam.identica.adapter.command.annotation.IdenticaAnnotationParser;
import me.whereareiam.identica.adapter.command.definition.CommandDefinitionAdapter;
import me.whereareiam.identica.adapter.command.executor.HelpCommand;
import me.whereareiam.identica.adapter.command.executor.MainCommand;
import me.whereareiam.identica.adapter.command.executor.ReloadCommand;
import me.whereareiam.identica.model.CommandDefinition;
import me.whereareiam.identica.command.CommandService;
import me.whereareiam.identica.model.config.Commands;
import me.whereareiam.keystone.Actor;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Singleton
public class DefaultCommandService implements CommandService {
	private final Provider<Commands> commandsProvider;
	private final Provider<CommandManager<Actor>> commandManagerProvider;
	private final Injector injector;

	private final Map<String, CommandDefinition> registeredDefinitions = new HashMap<>();
	private IdenticaAnnotationParser<Actor> annotationParser;

	@Inject
	public DefaultCommandService(
			Provider<Commands> commandsProvider,
			Provider<CommandManager<Actor>> commandManagerProvider,
			Injector injector
	) {
		this.commandsProvider = commandsProvider;
		this.commandManagerProvider = commandManagerProvider;
		this.injector = injector;

		initialize();
	}

	@Override
	public void registerCommand(@NotNull String key, @NotNull CommandDefinition definition, @NotNull Class<?> commandClass) {
		Object instance = injector.getInstance(commandClass);
		registerCommandInstance(key, definition, instance);
	}

	@Override
	public void registerCommands(@NotNull Map<String, CommandDefinition> definitions, @NotNull Class<?>... commandClasses) {
		Object[] instances = new Object[commandClasses.length];
		for (int i = 0; i < commandClasses.length; i++)
			instances[i] = injector.getInstance(commandClasses[i]);

		registerCommandInstances(definitions, instances);
	}

	@Override
	public void registerCommandInstance(@NotNull String key, @NotNull CommandDefinition definition, @NotNull Object commandInstance) {
		registeredDefinitions.put(key, definition);
		registerInternal(commandInstance);
	}

	@Override
	public void registerCommandInstances(@NotNull Map<String, CommandDefinition> definitions, @NotNull Object... commandInstances) {
		registeredDefinitions.putAll(definitions);
		registerInternal(commandInstances);
	}

	@Override
	public int getCommandCount() {
		return commandManagerProvider.get().commands().size();
	}

	@Override
	public @NotNull Map<String, CommandDefinition> getRegisteredDefinitions() {
		Map<String, CommandDefinition> all = new HashMap<>(registeredDefinitions);
		Commands commands = commandsProvider.get();
		if (commands != null && commands.getCommands() != null)
			commands.getCommands().forEach(all::putIfAbsent);

		return all;
	}

	private void registerInternal(Object... commandInstances) {
		CommandManager<Actor> commandManager = commandManagerProvider.get();
		if (annotationParser == null) {
			annotationParser = IdenticaAnnotationParser.create(commandManager, Actor.class);
		}

		Collection<Command<Actor>> parsed = annotationParser.parse(commandInstances);
		CommandDefinitionAdapter adapter = new CommandDefinitionAdapter();

		for (Command<Actor> command : parsed) {
			String defId = command.commandMeta().optional(CommandantKeys.DEFINITION_ID).orElse(null);
			CommandDefinition definition = defId != null ? lookupDefinition(defId) : null;

			Commandant.process(command, commandManager)
					.withDefinition(definition, adapter)
					.register();
		}
	}

	private CommandDefinition lookupDefinition(@NotNull String key) {
		CommandDefinition registered = registeredDefinitions.get(key);
		if (registered != null) return registered;
		Commands commands = commandsProvider.get();

		return commands.getCommands().get(key);
	}

	private void initialize() {
		registerInternal(
				injector.getInstance(MainCommand.class),
				injector.getInstance(HelpCommand.class),
				injector.getInstance(ReloadCommand.class)
		);
	}
}
