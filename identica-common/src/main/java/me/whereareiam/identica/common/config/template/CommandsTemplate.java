package me.whereareiam.identica.common.config.template;

import com.google.inject.Singleton;
import me.whereareiam.configura.TemplateProvider;
import me.whereareiam.identica.model.CommandDefinition;
import me.whereareiam.identica.model.config.Commands;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Singleton
public class CommandsTemplate implements TemplateProvider<Commands> {
	@Override
	public Commands supply(Commands commands) {
		Commands.Behavior behavior = new Commands.Behavior();

		Commands.Behavior.Clear clearSettings = new Commands.Behavior.Clear();
		clearSettings.setConfirmTtl(Duration.ofSeconds(60));
		behavior.setClear(clearSettings);

		Commands.Behavior.Sessions sessions = new Commands.Behavior.Sessions();
		sessions.setListPageSize(7);
		behavior.setSessions(sessions);
		commands.setBehavior(behavior);

		CommandDefinition main = CommandDefinition.builder()
				.enabled(true)
				.aliases(List.of("identica", "auth"))
				.permission("")
				.description("Main command")
				.usage("{alias}")
				.cooldown(CommandDefinition.Cooldown.builder()
						.enabled(true)
						.duration(2)
						.group("global")
						.build())
				.build();

		CommandDefinition reload = CommandDefinition.builder()
				.enabled(true)
				.aliases(List.of("reload"))
				.permission("identica.admin")
				.description("Reload command")
				.usage("{command} {alias}")
				.cooldown(CommandDefinition.Cooldown.builder()
						.enabled(true)
						.duration(2)
						.group("global")
						.build())
				.build();

		CommandDefinition help = CommandDefinition.builder()
				.enabled(true)
				.aliases(List.of("help"))
				.permission("")
				.description("Help command")
				.usage("{command} {alias} [page]")
				.arguments(Map.of("page", "Page"))
				.cooldown(CommandDefinition.Cooldown.builder()
						.enabled(true)
						.duration(2)
						.group("global")
						.build())
				.build();

		CommandDefinition clear = CommandDefinition.builder()
				.enabled(true)
				.aliases(List.of("clear"))
				.permission("identica.admin.clear")
				.description("Clear account data")
				.usage("{command} {alias} <target>")
				.arguments(Map.of("target", "Player/UUID"))
				.cooldown(CommandDefinition.Cooldown.builder()
						.enabled(true)
						.duration(2)
						.group("global")
						.build())
				.build();

		CommandDefinition clearCache = CommandDefinition.builder()
				.enabled(true)
				.aliases(List.of("clear cache"))
				.permission("identica.admin.clear.cache")
				.description("Clear account cache")
				.usage("{command} {alias} <target>")
				.arguments(Map.of("target", "Player/UUID"))
				.cooldown(CommandDefinition.Cooldown.builder()
						.enabled(true)
						.duration(2)
						.group("global")
						.build())
				.build();

		CommandDefinition clearConfirm = CommandDefinition.builder()
				.enabled(true)
				.aliases(List.of("clear confirm"))
				.permission("")
				.description("Confirm account clear")
				.usage("{command} {alias}")
				.hide(true)
				.build();

		CommandDefinition clearCancel = CommandDefinition.builder()
				.enabled(true)
				.aliases(List.of("clear cancel"))
				.permission("")
				.description("Cancel account clear")
				.usage("{command} {alias}")
				.hide(true)
				.build();

		CommandDefinition session = CommandDefinition.builder()
				.enabled(true)
				.aliases(List.of("session"))
				.permission("identica.admin.sessions")
				.description("List active sessions")
				.usage("{command} {alias} [page]")
				.arguments(Map.of("page", "Page"))
				.hide(true)
				.cooldown(CommandDefinition.Cooldown.builder()
						.enabled(true)
						.duration(2)
						.group("global")
						.build())
				.build();

		CommandDefinition sessionList = CommandDefinition.builder()
				.enabled(true)
				.aliases(List.of("session list"))
				.permission("identica.admin.sessions")
				.description("List active sessions")
				.usage("{command} {alias} [page]")
				.arguments(Map.of("page", "Page"))
				.cooldown(CommandDefinition.Cooldown.builder()
						.enabled(true)
						.duration(2)
						.group("global")
						.build())
				.build();

		CommandDefinition sessionInfo = CommandDefinition.builder()
				.enabled(true)
				.aliases(List.of("session info"))
				.permission("identica.admin.sessions.info")
				.description("Show session details")
				.usage("{command} {alias} <target>")
				.arguments(Map.of("target", "Player/UUID"))
				.cooldown(CommandDefinition.Cooldown.builder()
						.enabled(true)
						.duration(2)
						.group("global")
						.build())
				.build();

		CommandDefinition sessionEnd = CommandDefinition.builder()
				.enabled(true)
				.aliases(List.of("session end"))
				.permission("identica.admin.sessions.end")
				.description("End an active session")
				.usage("{command} {alias} <target>")
				.arguments(Map.of("target", "Player/UUID"))
				.cooldown(CommandDefinition.Cooldown.builder()
						.enabled(true)
						.duration(2)
						.group("global")
						.build())
				.build();

		commands.getCommands().putAll(Stream.of(
				main, reload, help,
				clear, clearCache, clearConfirm, clearCancel,
				session, sessionList, sessionInfo, sessionEnd
		).collect(Collectors.toMap(cmd -> cmd.getAliases().getFirst(), cmd -> cmd)));

		return commands;
	}
}
