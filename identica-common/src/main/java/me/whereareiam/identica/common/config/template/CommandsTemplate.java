package me.whereareiam.identica.common.config.template;

import com.google.inject.Singleton;
import me.whereareiam.configura.TemplateProvider;
import me.whereareiam.identica.model.CommandDefinition;
import me.whereareiam.identica.model.config.Commands;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

		Commands.Behavior.Migration migrationBehavior = new Commands.Behavior.Migration();
		migrationBehavior.setConfirmTtl(Duration.ofSeconds(60));
		behavior.setMigration(migrationBehavior);

		Commands.Behavior.Suggestions suggestions = new Commands.Behavior.Suggestions();
		suggestions.setPlayerLimit(25);
		behavior.setSuggestions(suggestions);
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
				.hide(true)
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

		CommandDefinition enroll = CommandDefinition.builder()
				.enabled(true)
				.aliases(List.of("enroll"))
				.permission("")
				.description("Select authentication eligibility")
				.usage("{command} {alias} <eligibility>")
				.arguments(Map.of("eligibility", "Provider id"))
				.hide(true)
				.build();

		CommandDefinition availabilityUsername = CommandDefinition.builder()
				.enabled(true)
				.aliases(List.of("availability username"))
				.permission("")
				.description("Check username availability")
				.usage("{command} {alias} <username>")
				.arguments(Map.of("username", "Username"))
				.cooldown(CommandDefinition.Cooldown.builder()
						.enabled(true)
						.duration(2)
						.group("global")
						.build())
				.build();

		CommandDefinition migrationList = CommandDefinition.builder()
				.enabled(true)
				.aliases(List.of("migration list"))
				.permission("identica.admin.migration.list")
				.description("List provider links for a player")
				.usage("{command} {alias} <target>")
				.arguments(Map.of("target", "Player/UUID"))
				.cooldown(CommandDefinition.Cooldown.builder()
						.enabled(true)
						.duration(2)
						.group("global")
						.build())
				.build();

		CommandDefinition migrationStart = CommandDefinition.builder()
				.enabled(true)
				.aliases(List.of("migration start"))
				.permission("identica.admin.migration.start")
				.description("Start provider migration for a player")
				.usage("{command} {alias} <target> <provider>")
				.arguments(Map.of("target", "Player/UUID", "provider", "Provider id"))
				.cooldown(CommandDefinition.Cooldown.builder()
						.enabled(true)
						.duration(2)
						.group("global")
						.build())
				.build();

		CommandDefinition migrationCancel = CommandDefinition.builder()
				.enabled(true)
				.aliases(List.of("migration cancel"))
				.permission("identica.admin.migration.cancel")
				.description("Cancel provider migration for a player")
				.usage("{command} {alias} <target>")
				.arguments(Map.of("target", "Player/UUID"))
				.cooldown(CommandDefinition.Cooldown.builder()
						.enabled(true)
						.duration(2)
						.group("global")
						.build())
				.build();

		CommandDefinition migrationPrimary = CommandDefinition.builder()
				.enabled(true)
				.aliases(List.of("migration primary"))
				.permission("identica.admin.migration.primary")
				.description("Set primary provider for a player")
				.usage("{command} {alias} <target> <provider>")
				.arguments(Map.of("target", "Player/UUID", "provider", "Provider id"))
				.cooldown(CommandDefinition.Cooldown.builder()
						.enabled(true)
						.duration(2)
						.group("global")
						.build())
				.build();

		CommandDefinition migrationDrop = CommandDefinition.builder()
				.enabled(true)
				.aliases(List.of("migration drop"))
				.permission("identica.admin.migration.drop")
				.description("Drop provider link for a player")
				.usage("{command} {alias} <target> <provider>")
				.arguments(Map.of("target", "Player/UUID", "provider", "Provider id"))
				.cooldown(CommandDefinition.Cooldown.builder()
						.enabled(true)
						.duration(2)
						.group("global")
						.build())
				.build();

		CommandDefinition verification = CommandDefinition.builder()
				.enabled(true)
				.aliases(List.of("2fa"))
				.permission("")
				.description("Verification methods")
				.usage("{alias}")
				.build();

		CommandDefinition verificationStatus = CommandDefinition.builder()
				.enabled(true)
				.aliases(List.of("2fa status"))
				.permission("")
				.description("Show verification status")
				.usage("{alias}")
				.build();

		CommandDefinition verificationEnroll = CommandDefinition.builder()
				.enabled(true)
				.aliases(List.of("2fa enroll"))
				.permission("")
				.description("Start verification method enrollment")
				.usage("{alias} <method>")
				.arguments(Map.of("method", "Method id"))
				.build();

		CommandDefinition verificationConfirm = CommandDefinition.builder()
				.enabled(true)
				.aliases(List.of("2fa confirm"))
				.permission("")
				.description("Confirm verification challenge or enrollment")
				.usage("{alias} <input>")
				.arguments(Map.of("input", "Code or saved"))
				.build();

		CommandDefinition verificationUse = CommandDefinition.builder()
				.enabled(true)
				.aliases(List.of("2fa use"))
				.permission("")
				.description("Select a verification method for a provider")
				.usage("{alias} <provider> <method>")
				.arguments(Map.of("provider", "Provider id", "method", "Method id"))
				.build();

		CommandDefinition verificationDisable = CommandDefinition.builder()
				.enabled(true)
				.aliases(List.of("2fa disable"))
				.permission("")
				.description("Disable an enrolled verification method")
				.usage("{alias} <method>")
				.arguments(Map.of("method", "Method id"))
				.build();

		CommandDefinition verificationCancel = CommandDefinition.builder()
				.enabled(true)
				.aliases(List.of("2fa cancel"))
				.permission("")
				.description("Cancel pending verification enrollment")
				.usage("{alias}")
				.build();

		CommandDefinition verificationReset = CommandDefinition.builder()
				.enabled(true)
				.aliases(List.of("2fa reset"))
				.permission("identica.admin.2fa.reset")
				.description("Reset verification state for a player")
				.usage("{command} {alias} <target> [provider]")
				.arguments(Map.of("target", "Player/UUID", "provider", "Provider id"))
				.build();

		Map<String, CommandDefinition> definitions = new LinkedHashMap<>();
		definitions.put("main", main);
		definitions.put("reload", reload);
		definitions.put("help", help);

		definitions.put("clear", clear);
		definitions.put("clear-cache", clearCache);
		definitions.put("clear-confirm", clearConfirm);
		definitions.put("clear-cancel", clearCancel);

		definitions.put("session", session);
		definitions.put("session-list", sessionList);
		definitions.put("session-info", sessionInfo);
		definitions.put("session-end", sessionEnd);

		definitions.put("enroll", enroll);
		definitions.put("availability-username", availabilityUsername);

		definitions.put("migration-list", migrationList);
		definitions.put("migration-start", migrationStart);
		definitions.put("migration-cancel", migrationCancel);
		definitions.put("migration-primary", migrationPrimary);
		definitions.put("migration-drop", migrationDrop);
		definitions.put("verification", verification);
		definitions.put("verification-status", verificationStatus);
		definitions.put("verification-enroll", verificationEnroll);
		definitions.put("verification-confirm", verificationConfirm);
		definitions.put("verification-use", verificationUse);
		definitions.put("verification-disable", verificationDisable);
		definitions.put("verification-cancel", verificationCancel);
		definitions.put("verification-reset", verificationReset);

		commands.getCommands().putAll(definitions);

		return commands;
	}
}
