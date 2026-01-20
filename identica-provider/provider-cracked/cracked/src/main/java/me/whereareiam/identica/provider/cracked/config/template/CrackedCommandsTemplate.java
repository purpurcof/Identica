package me.whereareiam.identica.provider.cracked.config.template;

import com.google.inject.Singleton;
import me.whereareiam.configura.TemplateProvider;
import me.whereareiam.identica.model.CommandDefinition;
import me.whereareiam.identica.model.config.Commands;

import java.util.List;
import java.util.Map;

@Singleton
public class CrackedCommandsTemplate implements TemplateProvider<Commands> {
	@Override
	public Commands supply(Commands commands) {
		CommandDefinition register = CommandDefinition.builder()
				.enabled(true)
				.aliases(List.of("register", "reg"))
				.permission("")
				.description("Register a cracked account")
				.usage("{alias} <password> <repeat>")
				.cooldown(CommandDefinition.Cooldown.builder()
						.enabled(true)
						.duration(2)
						.group("global")
						.build()
				)
				.arguments(Map.of("password", "Password", "repeat", "Repeat"))
				.build();

		CommandDefinition login = CommandDefinition.builder()
				.enabled(true)
				.aliases(List.of("login", "l"))
				.permission("")
				.description("Login to a cracked account")
				.usage("{alias} <password>")
				.cooldown(CommandDefinition.Cooldown.builder()
						.enabled(true)
						.duration(2)
						.group("global")
						.build()
				)
				.arguments(Map.of("password", "Password"))
				.build();

		CommandDefinition changePassword = CommandDefinition.builder()
				.enabled(true)
				.aliases(List.of("changepassword", "changepass", "password"))
				.permission("")
				.description("Change cracked account password")
				.usage("{alias} <current> <new> <repeat>")
				.cooldown(CommandDefinition.Cooldown.builder()
						.enabled(true)
						.duration(2)
						.group("global")
						.build()
				)
				.arguments(Map.of("current", "Current", "new", "New", "repeat", "Repeat"))
				.build();

		CommandDefinition adminDelete = CommandDefinition.builder()
				.enabled(true)
				.aliases(List.of("cracked delete"))
				.permission("identica.admin")
				.description("Delete a cracked account")
				.usage("{command} {alias} <username>")
				.cooldown(CommandDefinition.Cooldown.builder()
						.enabled(true)
						.duration(2)
						.group("global")
						.build()
				)
				.arguments(Map.of("username", "Username"))
				.build();

		CommandDefinition adminForceRegister = CommandDefinition.builder()
				.enabled(true)
				.aliases(List.of("cracked register"))
				.permission("identica.admin")
				.description("Force register a cracked account")
				.usage("{command} {alias} <username> <password>")
				.cooldown(CommandDefinition.Cooldown.builder()
						.enabled(true)
						.duration(2)
						.group("global")
						.build()
				)
				.arguments(Map.of("username", "Username", "password", "Password"))
				.build();

		CommandDefinition adminDropSessions = CommandDefinition.builder()
				.enabled(true)
				.aliases(List.of("cracked dropsession"))
				.permission("identica.admin")
				.description("Drop cracked sessions for a user")
				.usage("{command} {alias} <username>")
				.cooldown(CommandDefinition.Cooldown.builder()
						.enabled(true)
						.duration(2)
						.group("global")
						.build()
				)
				.arguments(Map.of("username", "Username"))
				.build();

		CommandDefinition adminSetPassword = CommandDefinition.builder()
				.enabled(true)
				.aliases(List.of("cracked setpassword"))
				.permission("identica.admin")
				.description("Set a cracked account password")
				.usage("{command} {alias} <username> <password>")
				.cooldown(CommandDefinition.Cooldown.builder()
						.enabled(true)
						.duration(2)
						.group("global")
						.build()
				)
				.arguments(Map.of("username", "Username", "password", "Password"))
				.build();

		commands.getCommands().put("register", register);
		commands.getCommands().put("login", login);
		commands.getCommands().put("change_password", changePassword);
		commands.getCommands().put("admin_delete", adminDelete);
		commands.getCommands().put("admin_force_register", adminForceRegister);
		commands.getCommands().put("admin_drop_session", adminDropSessions);
		commands.getCommands().put("admin_set_password", adminSetPassword);

		return commands;
	}
}
