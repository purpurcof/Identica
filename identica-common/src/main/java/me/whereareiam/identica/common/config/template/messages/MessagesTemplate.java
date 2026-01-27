package me.whereareiam.identica.common.config.template.messages;

import com.google.inject.Singleton;
import me.whereareiam.configura.TemplateProvider;
import me.whereareiam.identica.model.config.DateTimePattern;
import me.whereareiam.identica.model.config.Messages;

import java.util.List;

@Singleton
public class MessagesTemplate implements TemplateProvider<Messages> {
	@Override
	public Messages supply(Messages messages) {
		messages.setPrefix("<green>Identica</green> <dark_gray>| ");
		Messages.Format format = new Messages.Format();
		Messages.Format.Temporal temporal = new Messages.Format.Temporal();
		temporal.setDate(new DateTimePattern("dd.MM.yyyy"));
		temporal.setDateTime(new DateTimePattern("dd.MM.yyyy HH:mm:ss"));
		format.setTemporal(temporal);
		messages.setFormat(format);

		// Commands
		Messages.Commands commands = new MessagesCommandsTemplate().supply(new Messages.Commands());
		messages.setCommands(commands);

		// Providers
		Messages.Providers providers = new Messages.Providers();
		providers.setNoProvidersAvailable(List.of(
				"<green>ɪᴅᴇɴᴛɪᴄᴀ",
				"",
				"<white>No providers available, if this issue persists",
				"<white>please report it to the server administrator.</white>",
				"",
				"<gray>discord.arcadeya.com"
		));
		providers.setNoProvidersMatched(List.of(
				"<green>ɪᴅᴇɴᴛɪᴄᴀ",
				"",
				"<white>No providers matched, if this issue persists",
				"<white>please report it to the server administrator.</white>",
				"",
				"<gray>discord.arcadeya.com"
		));
		messages.setProviders(providers);

		// Authentication
		Messages.Authentication authentication = new Messages.Authentication();
		authentication.setHandshakeDenied(List.of(
				"<green>ɪᴅᴇɴᴛɪᴄᴀ",
				"",
				"<white>Handshake denied.</white>",
				"",
				"<gray>discord.arcadeya.com"
		));
		authentication.setAuthenticationFailed(List.of(
				"<green>ɪᴅᴇɴᴛɪᴄᴀ",
				"",
				"<white>Authentication failed.</white>",
				"",
				"<gray>discord.arcadeya.com"
		));
		authentication.setNoCompletionStep(List.of(
				"<green>ɪᴅᴇɴᴛɪᴄᴀ",
				"",
				"<white>Authentication pipeline incomplete.</white>",
				"",
				"<gray>discord.arcadeya.com"
		));
		authentication.setStepNoStatus(List.of(
				"<green>ɪᴅᴇɴᴛɪᴄᴀ",
				"",
				"<white>Authentication step returned no status.</white>",
				"",
				"<gray>discord.arcadeya.com"
		));
		messages.setAuthentication(authentication);

		return messages;
	}
}
