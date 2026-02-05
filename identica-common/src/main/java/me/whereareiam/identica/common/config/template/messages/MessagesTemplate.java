package me.whereareiam.identica.common.config.template.messages;

import com.google.inject.Singleton;
import me.whereareiam.configura.TemplateProvider;
import me.whereareiam.identica.model.config.DateTimePattern;
import me.whereareiam.identica.model.config.Messages;

import java.util.List;
import java.util.Map;

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
		authentication.setConcurrentLoginKick(List.of(
				"<green>ɪᴅᴇɴᴛɪᴄᴀ",
				"",
				"<white>You logged in from another location.</white>",
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
		Messages.Authentication.Steps steps = new Messages.Authentication.Steps();
		Messages.Authentication.Steps.Enrollment enrollment = new Messages.Authentication.Steps.Enrollment();
		enrollment.setTitle(List.of());
		enrollment.setBody(List.of(
				" ",
				" <green><bold>Identica</bold>",
				"  <white>Select an authentication method for account</white>",
				"  <white>It can be changed later on.</white>",
				" ",
				"  <gray>Available providers:</gray>",
				"{entries}",
				" "
		));
		Messages.Authentication.Steps.Enrollment.EntryFormat enrollmentEntry = new Messages.Authentication.Steps.Enrollment.EntryFormat();
		enrollmentEntry.setFormat("   <dark_gray><click:run_command:/identica enroll {providerId}>▪ <gray>[{providerName}]:</gray> <white>{description}</click>");
		enrollmentEntry.setEmptyFormat("   <dark_gray><click:run_command:/identica enroll {providerId}>▪ <gray>[{providerName}]:</gray></click>");
		enrollment.setEntryFormat(enrollmentEntry);
		enrollment.setEmpty(List.of(
				"<green>ɪᴅᴇɴᴛɪᴄᴀ",
				"",
				"<white>No providers available for this account.</white>",
				"",
				"<gray>discord.arcadeya.com"
		));
		enrollment.setDescriptions(Map.of(
				"premium", "Use Minecraft account for registration.",
				"cracked", "Register using password."
		));
		steps.setEnrollment(enrollment);
		authentication.setSteps(steps);
		Messages.Authentication.Routing routingMessages = new Messages.Authentication.Routing();
		routingMessages.setMissingServer(List.of(
				"<green>ɪᴅᴇɴᴛɪᴄᴀ",
				"",
				"<white>No target server available.</white>",
				"<white>Please contact a server administrator.</white>",
				"",
				"<gray>discord.arcadeya.com"
		));
		authentication.setRouting(routingMessages);
		messages.setAuthentication(authentication);

		return messages;
	}
}
