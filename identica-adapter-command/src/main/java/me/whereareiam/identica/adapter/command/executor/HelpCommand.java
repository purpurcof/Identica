package me.whereareiam.identica.adapter.command.executor;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import me.whereareiam.commandant.Help;
import me.whereareiam.commandant.CommandantKeys;
import me.whereareiam.commandant.builder.HelpBuilder;
import me.whereareiam.identica.Reloadable;
import me.whereareiam.identica.Serializer;
import me.whereareiam.identica.annotation.Argument;
import me.whereareiam.identica.annotation.Command;
import me.whereareiam.identica.annotation.Default;
import me.whereareiam.identica.annotation.Definition;
import me.whereareiam.identica.annotation.Range;
import me.whereareiam.identica.model.CommandDefinition;
import me.whereareiam.identica.model.config.Commands;
import me.whereareiam.identica.model.config.Messages;
import me.whereareiam.identica.registry.Registry;
import me.whereareiam.keystone.Actor;
import me.whereareiam.keystone.model.SerializerOptions;
import net.kyori.adventure.text.Component;
import org.incendo.cloud.CommandManager;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

@Singleton
public class HelpCommand implements Reloadable {
	private final Provider<Commands> commandsProvider;
	private final Provider<Messages> messagesProvider;
	private final Provider<CommandManager<Actor>> commandManagerProvider;

	private HelpBuilder<Actor> helpBuilder;

	@Inject
	public HelpCommand(
			@NotNull Provider<Commands> commandsProvider,
			@NotNull Provider<Messages> messagesProvider,
			@NotNull Provider<CommandManager<Actor>> commandManagerProvider,
			@NotNull Registry<Reloadable> reloadableRegistry
	) {
		this.commandsProvider = commandsProvider;
		this.messagesProvider = messagesProvider;
		this.commandManagerProvider = commandManagerProvider;
		reloadableRegistry.register(this);
	}

	@Definition("help")
	@Command("identica help [page]")
	public void command(@NotNull Actor sender, @Argument("page") @Default("1") @Range(min = "1") int page) {
		String helpMessage = getHelpBuilder().build(getFilteredCommands(sender), page);
		Component component = Serializer.serialize(sender, helpMessage);
		sender.sendMessage(component);
	}

	@NotNull
	private HelpBuilder<Actor> getHelpBuilder() {
		if (helpBuilder == null) {
			Messages messages = messagesProvider.get();
			SerializerOptions.PlaceholderFormat placeholderFormat = Serializer.getEngine().getPlaceholderFormat();

			helpBuilder = Help.<Actor>builder(messages.getCommands().getHelp())
					.customArgumentNames(collectArgumentDescriptions())
					.paginationMessages(messages.getCommands().getPagination())
					.sortAlphabetically(true)
					.dedupeByDefinitionId(true)
					.placeholderFormat(placeholderFormat)
					.build();
		}
		return helpBuilder;
	}

	@NotNull
	private Map<String, String> collectArgumentDescriptions() {
		return commandsProvider.get().getCommands().values().stream()
				.map(CommandDefinition::getArguments)
				.filter(map -> map != null && !map.isEmpty())
				.flatMap(map -> map.entrySet().stream())
				.collect(Collectors.toMap(
						Map.Entry::getKey,
						Map.Entry::getValue,
						(_, replacement) -> replacement
				));
	}

	@NotNull
	private Collection<org.incendo.cloud.Command<Actor>> getFilteredCommands(@NotNull Actor sender) {
		CommandManager<Actor> commandManager = commandManagerProvider.get();
		Map<String, CommandDefinition> definitions = commandsProvider.get().getCommands();

		return commandManager.commands()
				.stream()
				.filter(command -> {
					String definitionId = command.commandMeta()
							.optional(CommandantKeys.DEFINITION_ID)
							.orElse(null);
					if (definitionId == null) return true;
					CommandDefinition definition = definitions.get(definitionId);
					return definition == null || !definition.isHide();
				})
				.filter(command -> commandManager.hasPermission(sender, command.commandPermission().permissionString()))
				.collect(Collectors.toList());
	}

	@Override
	public void reload() {
		helpBuilder = null;
	}
}
