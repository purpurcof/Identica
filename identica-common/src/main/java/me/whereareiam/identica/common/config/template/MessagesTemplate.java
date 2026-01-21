package me.whereareiam.identica.common.config.template;

import com.google.inject.Singleton;
import me.whereareiam.commandant.model.message.ExceptionMessages;
import me.whereareiam.commandant.model.message.HelpMessages;
import me.whereareiam.commandant.model.message.PaginationMessages;
import me.whereareiam.configura.TemplateProvider;
import me.whereareiam.identica.model.config.Messages;

import java.util.List;

@Singleton
public class MessagesTemplate implements TemplateProvider<Messages> {
	@Override
	public Messages supply(Messages messages) {
		messages.setPrefix("<gold>ɪᴅᴇɴᴛɪᴄᴀ</gold> <dark_gray>| ");

		// Configure command messages
		Messages.Commands commands = new Messages.Commands();

		// Configure command exception messages
		ExceptionMessages exceptionMessages = new ExceptionMessages();
		exceptionMessages.setNoPermission("{prefix}<white>You don't have \"<gray>{content}</gray>\" permission to use this command.</white>");
		exceptionMessages.setExecutionError("{prefix}<white>An error occurred while executing the command:</white> <gray>{content}</gray>");
		exceptionMessages.setInvalidSyntax("{prefix}<white>Invalid syntax, please use:</white> <gray>/{content}</gray>");
		exceptionMessages.setInvalidSyntaxBoolean("{prefix}<white>You tried to use <gray>{content}</gray> as a boolean, but it's not a valid value, please use <green>true</green> or <red>false</red>.</white>");
		exceptionMessages.setInvalidSyntaxNumber("{prefix}<white>You tried to use <gray>{content}</gray> as a number, but it's not a valid value, please use a valid number.</white>");
		exceptionMessages.setInvalidSyntaxString("{prefix}<white>You tried to use <gray>{content}</gray> as a string, but it's not a valid value, please use a valid string.</white>");
		exceptionMessages.setInvalidSender("{prefix}<white>You cannot execute this command from this context.</white>");

		commands.setExceptions(exceptionMessages);

		// Configure pagination messages
		PaginationMessages paginationMessages = new PaginationMessages();
		paginationMessages.setShowPaginationIfOnePage(false);
		paginationMessages.setFormat("\n {previous}<white>Pagination</white> <gray>[{current}/{max}]</gray>{next} \n");
		paginationMessages.setShowPreviousEvenIfFirst(false);
		paginationMessages.setPreviousTagFormat("<red><click:run_command:/identica help {previousPage}>«</red> ");
		paginationMessages.setShowNextEvenIfLast(false);
		paginationMessages.setNextTagFormat(" <green><click:run_command:/identica help {nextPage}>»</green>");

		commands.setPagination(paginationMessages);

		// Configure help messages
		HelpMessages helpMessages = new HelpMessages();
		helpMessages.setFormat(List.of(
				" ",
				"<aqua><bold> Identica</bold> <white>Command help",
				" ",
				"{commands}",
				"{pagination}"
		));
		helpMessages.setCommandFormat("  <yellow>/{command}{arguments}</yellow> <dark_gray>- <white>{description}");
		helpMessages.setNoCommands("  <red>No commands found</red>");
		helpMessages.setCommandsPerPage(7);

		// Configure argument formatting
		HelpMessages.Format argumentFormat = new HelpMessages.Format();
		argumentFormat.setArgument("<gray>[{argument}]</gray>");
		argumentFormat.setOptionalArgument("<gray>({argument})</gray>");
		helpMessages.setArgumentFormat(argumentFormat);

		commands.setHelp(helpMessages);

		Messages.Commands.Reload reload = new Messages.Commands.Reload();
		reload.setSuccess("{prefix}<white>Configuration reloaded <green>successfully</green>!");
		reload.setError("{prefix}<white>An <red>error occurred</red> while reloading: <gray><error></gray>");
		commands.setReload(reload);

		messages.setCommands(commands);

		Messages.Providers providers = new Messages.Providers();
		providers.setNoProvidersAvailable(List.of(
				"<gold>ɪᴅᴇɴᴛɪᴄᴀ",
				"",
				"<white>No providers available, if this issue persists",
				"<white>please report it to the server administrator.</white>",
				"",
				"<gray>discord.arcadeya.com"
		));
		providers.setNoProvidersMatched(List.of(
				"<gold>ɪᴅᴇɴᴛɪᴄᴀ",
				"",
				"<white>No providers matched, if this issue persists",
				"<white>please report it to the server administrator.</white>",
				"",
				"<gray>discord.arcadeya.com"
		));
		messages.setProviders(providers);

		Messages.Authentication authentication = new Messages.Authentication();
		authentication.setHandshakeDenied(List.of(
				"<gold>ɪᴅᴇɴᴛɪᴄᴀ",
				"",
				"<white>Handshake denied.</white>",
				"",
				"<gray>discord.arcadeya.com"
		));
		authentication.setAuthenticationFailed(List.of(
				"<gold>ɪᴅᴇɴᴛɪᴄᴀ",
				"",
				"<white>Authentication failed.</white>",
				"",
				"<gray>discord.arcadeya.com"
		));
		authentication.setNoCompletionStep(List.of(
				"<gold>ɪᴅᴇɴᴛɪᴄᴀ",
				"",
				"<white>Authentication pipeline incomplete.</white>",
				"",
				"<gray>discord.arcadeya.com"
		));
		authentication.setStepNoStatus(List.of(
				"<gold>ɪᴅᴇɴᴛɪᴄᴀ",
				"",
				"<white>Authentication step returned no status.</white>",
				"",
				"<gray>discord.arcadeya.com"
		));
		messages.setAuthentication(authentication);

		return messages;
	}
}
