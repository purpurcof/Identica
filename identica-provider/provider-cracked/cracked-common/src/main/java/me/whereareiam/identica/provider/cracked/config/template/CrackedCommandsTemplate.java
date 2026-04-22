package me.whereareiam.identica.provider.cracked.config.template;

import com.google.inject.Singleton;
import me.whereareiam.configura.TemplateProvider;
import me.whereareiam.identica.model.CommandDefinition;
import me.whereareiam.identica.provider.cracked.config.CrackedCommands;

import java.util.List;
import java.util.Map;

@Singleton
public class CrackedCommandsTemplate implements TemplateProvider<CrackedCommands> {
	@Override
	public CrackedCommands supply(CrackedCommands commands) {
		CommandDefinition register = CommandDefinition.builder()
				.enabled(true)
				.aliases(List.of("pass"))
				.permission("")
				.description("Register a cracked account")
				.usage("{alias} <password>")
				.cooldown(CommandDefinition.Cooldown.builder()
						.enabled(true)
						.duration(2)
						.group("global")
						.build()
				)
				.arguments(Map.of("password", "Password"))
				.hide(true)
				.build();

		CommandDefinition registerConfirm = CommandDefinition.builder()
				.enabled(true)
				.aliases(List.of("passconfirm"))
				.permission("")
				.description("Confirm cracked registration")
				.usage("{alias} <password>")
				.cooldown(CommandDefinition.Cooldown.builder()
						.enabled(true)
						.duration(2)
						.group("global")
						.build()
				)
				.arguments(Map.of("password", "Repeat"))
				.hide(true)
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
				.hide(true)
				.build();

		CommandDefinition changePassword = CommandDefinition.builder()
				.enabled(true)
				.aliases(List.of("changepassword", "changepass", "password"))
				.permission("")
				.description("Change cracked account password")
				.usage("{alias} <current> <new> [repeat]")
				.cooldown(CommandDefinition.Cooldown.builder()
						.enabled(true)
						.duration(2)
						.group("global")
						.build()
				)
				.arguments(Map.of("current", "Current", "new", "New", "repeat", "Repeat"))
				.build();

		CommandDefinition cracked = CommandDefinition.builder()
				.enabled(true)
				.aliases(List.of("cracked"))
				.permission("")
				.description("Cracked account migration")
				.usage("{alias}")
				.cooldown(CommandDefinition.Cooldown.builder()
						.enabled(true)
						.duration(2)
						.group("global")
						.build()
				)
				.build();

		CommandDefinition crackedConfirm = CommandDefinition.builder()
				.enabled(true)
				.aliases(List.of("cracked confirm"))
				.permission("")
				.description("Confirm cracked migration")
				.usage("{alias} [code]")
				.hide(true)
				.build();

		CommandDefinition crackedCancel = CommandDefinition.builder()
				.enabled(true)
				.aliases(List.of("cracked cancel"))
				.permission("")
				.description("Cancel cracked migration")
				.usage("{alias}")
				.hide(true)
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

		commands.getCommands().put("pass", register);
		commands.getCommands().put("passconfirm", registerConfirm);
		commands.getCommands().put("login", login);
		commands.getCommands().put("change-password", changePassword);
		commands.getCommands().put("cracked", cracked);
		commands.getCommands().put("cracked-confirm", crackedConfirm);
		commands.getCommands().put("cracked-cancel", crackedCancel);
		commands.getCommands().put("admin-force-register", adminForceRegister);
		commands.getCommands().put("admin-set-password", adminSetPassword);

		return commands;
	}
}
