package me.whereareiam.identica.command;

import me.whereareiam.identica.model.CommandDefinition;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

@SuppressWarnings("unused")
public interface CommandService {
	void registerCommand(@NotNull String key, @NotNull CommandDefinition definition, @NotNull Class<?> commandClass);

	void registerCommands(@NotNull Map<String, CommandDefinition> definitions, @NotNull Class<?>... commandClasses);

	void registerCommandInstance(@NotNull String key, @NotNull CommandDefinition definition, @NotNull Object commandInstance);

	void registerCommandInstances(@NotNull Map<String, CommandDefinition> definitions, @NotNull Object... commandInstances);

	int getCommandCount();

	@NotNull
	Map<String, CommandDefinition> getRegisteredDefinitions();
}
