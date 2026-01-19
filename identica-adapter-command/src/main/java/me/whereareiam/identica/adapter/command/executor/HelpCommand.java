package me.whereareiam.identica.adapter.command.executor;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import me.whereareiam.commandant.Help;
import me.whereareiam.commandant.builder.HelpBuilder;
import me.whereareiam.identica.Reloadable;
import me.whereareiam.identica.model.CommandDefinition;
import me.whereareiam.identica.command.CommandService;
import me.whereareiam.identica.annotation.Argument;
import me.whereareiam.identica.annotation.Command;
import me.whereareiam.identica.annotation.Default;
import me.whereareiam.identica.annotation.Definition;
import me.whereareiam.identica.annotation.Range;
import me.whereareiam.identica.model.config.Messages;
import me.whereareiam.identica.registry.Registry;
import me.whereareiam.identica.Serializer;
import me.whereareiam.keystone.Actor;
import org.incendo.cloud.CommandManager;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

@Singleton
public class HelpCommand implements Reloadable {
	private final Provider<Messages> messagesProvider;
	private final Provider<CommandManager<Actor>> commandManagerProvider;
	private final CommandService commandService;

	private HelpBuilder<Actor> helpBuilder;
	private Map<String, String> cachedArgumentDescriptions;

	@Inject
	public HelpCommand(
			Provider<Messages> messagesProvider,
			Provider<CommandManager<Actor>> commandManagerProvider,
			CommandService commandService,
			Registry<Reloadable> reloadableRegistry
	) {
		this.messagesProvider = messagesProvider;
		this.commandManagerProvider = commandManagerProvider;
		this.commandService = commandService;
		reloadableRegistry.register(this);
	}

	@Definition("help")
	@Command("identica help [page]")
	public void command(@NotNull Actor sender, @Argument("page") @Default("1") @Range(min = "1") int page) {
		HelpBuilder<Actor> builder = getHelpBuilder();
		if (builder == null) return;

		String helpMessage = builder.build(getFilteredCommands(sender), page);
		sender.sendMessage(Serializer.serialize(sender, helpMessage));
	}

	private HelpBuilder<Actor> getHelpBuilder() {
		if (helpBuilder != null) return helpBuilder;
		Messages messages = messagesProvider.get();
		if (messages == null || messages.getCommands() == null || messages.getCommands().getHelp() == null) return null;

		helpBuilder = Help.<Actor>builder(messages.getCommands().getHelp())
				.customArgumentNames(getArgumentDescriptions())
				.paginationMessages(messages.getCommands().getPagination())
				.itemsPerPage(messages.getCommands().getHelp().getCommandsPerPage())
				.sortAlphabetically(true)
				.build();

		return helpBuilder;
	}

	private Map<String, String> getArgumentDescriptions() {
		if (cachedArgumentDescriptions != null) return cachedArgumentDescriptions;
		cachedArgumentDescriptions = collectArgumentDescriptions();

		return cachedArgumentDescriptions;
	}

	private Map<String, String> collectArgumentDescriptions() {
		return commandService.getRegisteredDefinitions().values().stream()
				.map(CommandDefinition::getArguments)
				.filter(map -> map != null && !map.isEmpty())
				.flatMap(map -> map.entrySet().stream())
				.collect(Collectors.toMap(
						Map.Entry::getKey,
						Map.Entry::getValue,
						(_, replacement) -> replacement
				));
	}

	private Collection<org.incendo.cloud.Command<Actor>> getFilteredCommands(@NotNull Actor sender) {
		CommandManager<Actor> commandManager = commandManagerProvider.get();
		return commandManager.commands()
				.stream()
				.filter(command -> commandManager.hasPermission(sender, command.commandPermission().permissionString()))
				.collect(Collectors.toList());
	}

	@Override
	public void reload() {
		helpBuilder = null;
		cachedArgumentDescriptions = null;
	}
}
