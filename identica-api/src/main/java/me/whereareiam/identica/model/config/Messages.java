package me.whereareiam.identica.model.config;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import me.whereareiam.commandant.model.message.HelpMessages;
import me.whereareiam.commandant.model.message.PaginationMessages;

@Getter
@Setter
@ToString
public class Messages {
	private String prefix = "<gold>ɪᴅᴇɴᴛɪᴄᴀ</gold> <dark_gray>| ";
	private Commands commands = new Commands();

	@Getter
	@Setter
	@ToString
	public static class Commands {
		private String main = "{prefix}<white>Identica is running.</white>";
		private String reloadStart = "{prefix}<white>Reloading configuration...</white>";
		private String reloadSuccess = "{prefix}<green>Reload complete.</green>";
		private String reloadError = "{prefix}<red>Reload failed: {error}</red>";
		private PaginationMessages pagination = new PaginationMessages();
		private HelpMessages help = new HelpMessages();
	}
}
