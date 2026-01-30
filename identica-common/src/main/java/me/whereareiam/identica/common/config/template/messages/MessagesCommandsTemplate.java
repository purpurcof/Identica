package me.whereareiam.identica.common.config.template.messages;

import com.google.inject.Singleton;
import me.whereareiam.commandant.model.message.ExceptionMessages;
import me.whereareiam.commandant.model.message.HelpMessages;
import me.whereareiam.commandant.model.message.PaginationMessages;
import me.whereareiam.identica.model.config.Messages;

import java.util.List;

@Singleton
public class MessagesCommandsTemplate {
	public Messages.Commands supply(Messages.Commands commands) {
		// Exceptions
		ExceptionMessages exceptionMessages = new ExceptionMessages();
		exceptionMessages.setNoPermission("{prefix}<white>You don't have \"<gray>{content}</gray>\" permission to use this command.</white>");
		exceptionMessages.setExecutionError("{prefix}<white>An error occurred while executing the command:</white> <gray>{content}</gray>");
		exceptionMessages.setInvalidSyntax("{prefix}<white>Invalid syntax, please use:</white> <gray>/{content}</gray>");
		exceptionMessages.setInvalidSyntaxBoolean("{prefix}<white>You tried to use <gray>{content}</gray> as a boolean, but it's not a valid value, please use <green>true</green> or <red>false</red>.</white>");
		exceptionMessages.setInvalidSyntaxNumber("{prefix}<white>You tried to use <gray>{content}</gray> as a number, but it's not a valid value, please use a valid number.</white>");
		exceptionMessages.setInvalidSyntaxString("{prefix}<white>You tried to use <gray>{content}</gray> as a string, but it's not a valid value, please use a valid string.</white>");
		exceptionMessages.setInvalidSender("{prefix}<white>You cannot execute this command from this context.</white>");
		commands.setExceptions(exceptionMessages);

		// Pagination
		PaginationMessages paginationMessages = new PaginationMessages();
		paginationMessages.setShowPaginationIfOnePage(false);
		paginationMessages.setFormat("\n {previous}<white>Pagination</white> <gray>[{current}/{max}]</gray>{next} \n");
		paginationMessages.setShowPreviousEvenIfFirst(false);
		paginationMessages.setPreviousTagFormat("<red><click:run_command:/identica help {previousPage}>«</red> ");
		paginationMessages.setShowNextEvenIfLast(false);
		paginationMessages.setNextTagFormat(" <green><click:run_command:/identica help {nextPage}>»</green>");
		commands.setPagination(paginationMessages);

		// Help
		HelpMessages helpMessages = new HelpMessages();
		helpMessages.setFormat(List.of(
				" ",
				" <green><bold>Identica</bold> <white>Command help",
				" ",
				"{commands}",
				"{pagination}"
		));
		helpMessages.setCommandFormat("  <yellow>/{command}{arguments}</yellow> <dark_gray>- <white>{description}");
		helpMessages.setNoCommands("  <red>No commands found</red>");
		helpMessages.setCommandsPerPage(7);

		HelpMessages.Format argumentFormat = new HelpMessages.Format();
		argumentFormat.setArgument("<gray>[{argument}]</gray>");
		argumentFormat.setOptionalArgument("<gray>({argument})</gray>");
		helpMessages.setArgumentFormat(argumentFormat);
		commands.setHelp(helpMessages);

		// Reload
		Messages.Commands.Reload reload = new Messages.Commands.Reload();
		reload.setSuccess("{prefix}<white>Configuration reloaded <green>successfully</green>!");
		reload.setError("{prefix}<white>An <red>error occurred</red> while reloading: <gray>{error}</gray>");
		commands.setReload(reload);

		// Clear
		Messages.Commands.Clear clear = new Messages.Commands.Clear();
		clear.setConfirm(List.of(
				" ",
				" <green><bold>Identica</bold>",
				" ",
				"  <white>Clear request for <aqua>{target}</aqua></white>",
				"  <white>Scope: <aqua>{scope}</aqua></white>",
				"  <white>UUID: <gray>{uniqueId}</gray></white>",
				" ",
				"  <click:run_command:/identica clear confirm><green>[CONFIRM]</green></click>       " +
						"<click:run_command:/identica clear cancel><red>[CANCEL]</red></click>",
				" "
		));
		clear.setNoPending("{prefix}<white>No pending clear request.</white>");
		clear.setExpired("{prefix}<white>Clear request expired, please run the command again.</white>");
		clear.setCancelled("{prefix}<white>Clear request cancelled.</white>");
		clear.setNotFound("{prefix}<white>No account found for <gray>{target}</gray>.</white>");
		Messages.Commands.Clear.Multiple clearMultiple = new Messages.Commands.Clear.Multiple();
		clearMultiple.setBody(List.of(
				" ",
				" <green><bold>Identica</bold>",
				" ",
				"  <white>Found <gray>{count}</gray> accounts for</white>",
				"  <white>username <gray>{target}</gray></white>",
				" ",
				"  <white>Matches:</white>",
				"{entries}",
				" "
		));
		Messages.Commands.EntryFormat clearEntryFormat = new Messages.Commands.EntryFormat();
		clearEntryFormat.setFormat("  <click:run_command:/{command} {uniqueId}>▪ <gray>{uniqueId}</gray>: <green>{username}</green></click>");
		clearEntryFormat.setEmptyFormat("  <click:run_command:/{command} {uniqueId}>▪ <gray>{uniqueId}</gray></click>");
		clearMultiple.setEntry(clearEntryFormat);
		clear.setMultiple(clearMultiple);
		clear.setSuccess("{prefix}<white>Cleared <gold>{scope}</gold> for <gray>{uniqueId}</gray>.</white>");
		clear.setError("{prefix}<white>An <red>error occurred</red> while clearing: <gray>{error}</gray></white>");
		clear.setDisconnect(List.of(
				"<green>ɪᴅᴇɴᴛɪᴄᴀ",
				"",
				"<white>Your session has been cleared.</white>",
				"<white>Please rejoin to continue.</white>",
				"",
				"<gray>discord.arcadeya.com"
		));
		commands.setClear(clear);

		// Sessions
		Messages.Commands.Sessions sessions = new Messages.Commands.Sessions();
		sessions.setUnknown("unknown");
		Messages.Commands.Sessions.Listing sessionList = new Messages.Commands.Sessions.Listing();
		sessionList.setBody(List.of(
				" ",
				" <green><bold>Identica</bold>",
				" ",
				"  <white>Active sessions:</white>",
				"{entries}",
				" "
		));
		Messages.Commands.EntryFormat sessionListEntry = new Messages.Commands.EntryFormat();
		sessionListEntry.setFormat("   <dark_gray>▪</dark_gray> <gray><click:run_command:/identica session info {uniqueId}>[{uniqueId}]</click></gray>\n     <white>Provider: <green>{eligibility} <gray>| <white>Username: <green>{username}");
		sessionListEntry.setEmptyFormat("  <dark_gray>▪</dark_gray> <gray><click:run_command:/identica session info {uniqueId}>[{uniqueId}]</click></gray>");
		sessionList.setEntry(sessionListEntry);
		sessionList.setEmpty("{prefix}<white>No active sessions.</white>");
		sessions.setListing(sessionList);

		Messages.Commands.Sessions.Info sessionInfo = new Messages.Commands.Sessions.Info();
		sessionInfo.setBody(List.of(
				" ",
				" <green><bold>Identica</bold>",
				" ",
				"  <white>Session information for <aqua>{username}</aqua></white>",
				" ",
				"  <gray>Information:</gray>",
				"   <white>UUID: <gray>{uniqueId}</gray></white>",
				"   <white>Subject: <gray>{subject}</gray></white>",
				"   <white>Session: <gray>{session}</gray></white>",
				" ",
				"   <white>Provider: <gray>{eligibility}</gray></white>",
				"   <white>IP: <gray>{ip}</gray></white>",
				" ",
				"   <white>Created: <gray>{created}</gray></white>",
				" ",
				"  <gray>Username:</gray>",
				"   <white>Original: <gray>{original}</gray></white>",
				"   <white>Effective: <gray>{effective}</gray></white>",
				" "
		));
		sessionInfo.setNotFound("{prefix}<white>No active session found for <gray>{target}</gray>.</white>");
		sessions.setInfo(sessionInfo);
		Messages.Commands.Sessions.Multiple sessionsMultiple = new Messages.Commands.Sessions.Multiple();
		sessionsMultiple.setBody(List.of(
				" ",
				" <green><bold>Identica</bold>",
				" ",
				"  <white>Found <gray>{count}</gray> sessions for</white>",
				"  <white>username <gray>{target}</gray></white>",
				" ",
				"  <white>Matches:</white>",
				"{entries}",
				" "
		));
		Messages.Commands.EntryFormat sessionsMultipleEntry = new Messages.Commands.EntryFormat();
		sessionsMultipleEntry.setFormat("  <click:run_command:/{command} {uniqueId}>▪ <gray>{uniqueId}</gray>: <green>{username}</green></click>");
		sessionsMultipleEntry.setEmptyFormat("  <click:run_command:/{command} {uniqueId}>▪ <gray>{uniqueId}</gray></click>");
		sessionsMultiple.setEntry(sessionsMultipleEntry);
		sessions.setMultiple(sessionsMultiple);
		Messages.Commands.Sessions.End sessionEnd = new Messages.Commands.Sessions.End();
		sessionEnd.setEnded("{prefix}<white>Ended session for <gray>{username}</gray> [<gray>{uniqueId}</gray>].</white>");
		sessionEnd.setNotFound("{prefix}<white>No active session found for <gray>{target}</gray>.</white>");
		sessionEnd.setDisconnect(List.of(
				"<green>ɪᴅᴇɴᴛɪᴄᴀ",
				"",
				"<white>Your session was ended.</white>",
				"<white>Please rejoin to continue.</white>",
				"",
				"<gray>discord.arcadeya.com"
		));
		sessions.setEnd(sessionEnd);
		commands.setSessions(sessions);

		return commands;
	}
}
