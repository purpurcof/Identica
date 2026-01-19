package me.whereareiam.identica.common.config.template;

import com.google.inject.Singleton;
import me.whereareiam.commandant.model.message.HelpMessages;
import me.whereareiam.commandant.model.message.PaginationMessages;
import me.whereareiam.configura.TemplateProvider;
import me.whereareiam.identica.model.config.Messages;

import java.util.List;

@Singleton
public class MessagesTemplate implements TemplateProvider<Messages> {
	@Override
	public Messages supply(Messages messages) {
		Messages.Commands commands = messages.getCommands();
		if (commands == null) {
			commands = new Messages.Commands();
			messages.setCommands(commands);
		}

		PaginationMessages paginationMessages = new PaginationMessages();
		paginationMessages.setShowPaginationIfOnePage(false);
		paginationMessages.setFormat("\n {previous}<white>Help</white> <gray>[{current}/{max}]</gray>{next} \n");
		paginationMessages.setShowPreviousEvenIfFirst(false);
		paginationMessages.setPreviousTagFormat("<red><click:run_command:/identica help {previousPage}>«</red> ");
		paginationMessages.setShowNextEvenIfLast(false);
		paginationMessages.setNextTagFormat(" <green><click:run_command:/identica help {nextPage}>»</green>");
		commands.setPagination(paginationMessages);

		HelpMessages helpMessages = new HelpMessages();
		helpMessages.setFormat(List.of(
				" ",
				"<gold><bold> Identica</bold> <white>Command help",
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

		return messages;
	}
}
