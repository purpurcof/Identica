package me.whereareiam.identica.common.config.defaults;

import com.google.inject.Singleton;
import me.whereareiam.configura.merge.defaults.DefaultsProvider;
import me.whereareiam.identica.common.config.defaults.commands.AdminCommandDefinitions;
import me.whereareiam.identica.common.config.defaults.commands.CoreCommandDefinitions;
import me.whereareiam.identica.common.config.defaults.commands.MigrationCommandDefinitions;
import me.whereareiam.identica.common.config.defaults.commands.base.CommandDefinitions;
import me.whereareiam.identica.model.config.Commands;

import java.time.Duration;
import java.util.List;

@Singleton
public class CommandsDefaults implements DefaultsProvider<Commands> {
	private final List<CommandDefinitions> definitions = List.of(
			new CoreCommandDefinitions(),
			new AdminCommandDefinitions(),
			new MigrationCommandDefinitions()
	);

	@Override
	public Commands supply(Commands commands) {
		commands.setBehavior(defaultBehavior());
		definitions.forEach(group -> group.register(commands.getCommands()::put));

		return commands;
	}

	private Commands.Behavior defaultBehavior() {
		Commands.Behavior behavior = new Commands.Behavior();
		behavior.setUseBrigadier(false);

		Commands.Behavior.Help help = new Commands.Behavior.Help();
		help.setSortAlphabetically(true);
		behavior.setHelp(help);

		Commands.Behavior.Clear clearSettings = new Commands.Behavior.Clear();
		clearSettings.setConfirmTtl(Duration.ofSeconds(60));
		behavior.setClear(clearSettings);

		Commands.Behavior.Sessions sessions = new Commands.Behavior.Sessions();
		sessions.setListPageSize(7);
		behavior.setSessions(sessions);

		Commands.Behavior.Migration migrationBehavior = new Commands.Behavior.Migration();
		migrationBehavior.setConfirmTtl(Duration.ofSeconds(60));
		behavior.setMigration(migrationBehavior);

		Commands.Behavior.Suggestions suggestions = new Commands.Behavior.Suggestions();
		suggestions.setPlayerLimit(25);
		behavior.setSuggestions(suggestions);

		return behavior;
	}
}
