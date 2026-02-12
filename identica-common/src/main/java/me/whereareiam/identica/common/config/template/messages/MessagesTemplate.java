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
				"<dark_gray>discord.arcadeya.com"
		));
		providers.setNoProvidersMatched(List.of(
				"<green>ɪᴅᴇɴᴛɪᴄᴀ",
				"",
				"<white>No providers matched, if this issue persists",
				"<white>please report it to the server administrator.</white>",
				"",
				"<dark_gray>discord.arcadeya.com"
		));
		messages.setProviders(providers);

		// Connection
		Messages.Connection connection = new Messages.Connection();
		connection.setConcurrentLoginKick(List.of(
				"<green>ɪᴅᴇɴᴛɪᴄᴀ",
				"",
				"<white>You logged in from another location.</white>",
				"",
				"<dark_gray>discord.arcadeya.com"
		));
		connection.setJourney(buildJourneyMessages());
		Messages.Connection.Authentication authentication = new Messages.Connection.Authentication();
		applyScenario(authentication, "Authentication");
		authentication.setAuthenticationFailed(List.of(
				"<green>ɪᴅᴇɴᴛɪᴄᴀ",
				"",
				"<white>Authentication failed.</white>",
				"",
				"<dark_gray>discord.arcadeya.com"
		));
		authentication.setSessionBuildFailed(List.of(
				"<green>ɪᴅᴇɴᴛɪᴄᴀ",
				"",
				"<white>Unable to start session.</white>",
				"",
				"<dark_gray>discord.arcadeya.com"
		));
		Messages.Connection.Registration registration = new Messages.Connection.Registration();
		applyScenario(registration, "Registration");
		registration.setRegistrationFailed(List.of(
				"<green>ɪᴅᴇɴᴛɪᴄᴀ",
				"",
				"<white>Registration failed.</white>",
				"",
				"<dark_gray>discord.arcadeya.com"
		));
		registration.setAccountAlreadyExists(List.of(
				"<green>ɪᴅᴇɴᴛɪᴄᴀ",
				"",
				"<white>Account already exists.</white>",
				"",
				"<dark_gray>discord.arcadeya.com"
		));
		connection.setAuthentication(authentication);
		connection.setRegistration(registration);
		messages.setConnection(connection);

		return messages;
	}

	private void applyScenario(
			Messages.Connection.Scenario scenario,
			String label
	) {
		scenario.setHandshakeDenied(List.of(
				"<green>ɪᴅᴇɴᴛɪᴄᴀ",
				"",
				"<white>Handshake denied.</white>",
				"",
				"<dark_gray>discord.arcadeya.com"
		));
		scenario.setPipelineKick(List.of(
				"<green>ɪᴅᴇɴᴛɪᴄᴀ",
				"",
				"<white>" + label + " pipeline already in progress.</white>",
				"",
				"<dark_gray>discord.arcadeya.com"
		));
		scenario.setNoCompletionPipeline(List.of(
				"<green>ɪᴅᴇɴᴛɪᴄᴀ",
				"",
				"<white>" + label + " pipeline incomplete.</white>",
				"",
				"<dark_gray>discord.arcadeya.com"
		));
		Messages.Connection.Routing routingMessages = new Messages.Connection.Routing();
		routingMessages.setMissingServer(List.of(
				"<green>ɪᴅᴇɴᴛɪᴄᴀ",
				"",
				"<white>No target server available.</white>",
				"<white>Please contact a server administrator.</white>",
				"",
				"<dark_gray>discord.arcadeya.com"
		));
		scenario.setRouting(routingMessages);
		scenario.setErrors(buildScenarioErrors(label));
	}

	private Messages.Connection.Scenario.Errors buildScenarioErrors(String label) {
		Messages.Connection.Scenario.Errors errors = new Messages.Connection.Scenario.Errors();
		errors.setPreparationMissingContext(List.of(
				"<green>ɪᴅᴇɴᴛɪᴄᴀ",
				"",
				"<white>Connection setup did not produce a context.</white>",
				"<white>Please try again.</white>",
				"",
				"<dark_gray>discord.arcadeya.com"
		));
		errors.setIdentityGroupMissingResult(List.of(
				"<green>ɪᴅᴇɴᴛɪᴄᴀ",
				"",
				"<white>" + label + " flow could not continue.</white>",
				"<white>Identity stage did not return a result.</white>",
				"",
				"<dark_gray>discord.arcadeya.com"
		));
		errors.setPolicyGroupMissingResult(List.of(
				"<green>ɪᴅᴇɴᴛɪᴄᴀ",
				"",
				"<white>" + label + " flow could not continue.</white>",
				"<white>Policy stage did not return a result.</white>",
				"",
				"<dark_gray>discord.arcadeya.com"
		));
		errors.setSessionGroupMissingResult(List.of(
				"<green>ɪᴅᴇɴᴛɪᴄᴀ",
				"",
				"<white>" + label + " flow could not continue.</white>",
				"<white>Session stage did not return a result.</white>",
				"",
				"<dark_gray>discord.arcadeya.com"
		));
		errors.setJourneyMissingResult(List.of(
				"<green>ɪᴅᴇɴᴛɪᴄᴀ",
				"",
				"<white>Journey did not return a result.</white>",
				"<white>Please try again.</white>",
				"",
				"<dark_gray>discord.arcadeya.com"
		));
		errors.setJourneyMissingContext(List.of(
				"<green>ɪᴅᴇɴᴛɪᴄᴀ",
				"",
				"<white>Journey could not start.</white>",
				"<white>Required context was missing.</white>",
				"",
				"<dark_gray>discord.arcadeya.com"
		));
		errors.setJourneyMissingPlan(List.of(
				"<green>ɪᴅᴇɴᴛɪᴄᴀ",
				"",
				"<white>Journey could not start.</white>",
				"<white>Execution plan was missing.</white>",
				"",
				"<dark_gray>discord.arcadeya.com"
		));
		errors.setFinalizeMissingResult(List.of(
				"<green>ɪᴅᴇɴᴛɪᴄᴀ",
				"",
				"<white>Finalization did not return a result.</white>",
				"<white>Please try again.</white>",
				"",
				"<dark_gray>discord.arcadeya.com"
		));
		errors.setIdentityProfileMissing(List.of(
				"<green>ɪᴅᴇɴᴛɪᴄᴀ",
				"",
				"<white>Unable to load provider profile.</white>",
				"<white>Required provider data is missing.</white>",
				"",
				"<dark_gray>discord.arcadeya.com"
		));
		errors.setAccountReviewMissing(List.of(
				"<green>ɪᴅᴇɴᴛɪᴄᴀ",
				"",
				"<white>Unable to review account.</white>",
				"<white>Required account data is missing.</white>",
				"",
				"<dark_gray>discord.arcadeya.com"
		));
		errors.setIdentityReplicationMissing(List.of(
				"<green>ɪᴅᴇɴᴛɪᴄᴀ",
				"",
				"<white>Unable to synchronize username.</white>",
				"<white>Required account data is missing.</white>",
				"",
				"<dark_gray>discord.arcadeya.com"
		));
		errors.setProviderValidationMissing(List.of(
				"<green>ɪᴅᴇɴᴛɪᴄᴀ",
				"",
				"<white>Unable to validate provider.</white>",
				"<white>Required provider data is missing.</white>",
				"",
				"<dark_gray>discord.arcadeya.com"
		));
		errors.setEnsureNewAccountMissing(List.of(
				"<green>ɪᴅᴇɴᴛɪᴄᴀ",
				"",
				"<white>Unable to verify account status.</white>",
				"<white>Required provider data is missing.</white>",
				"",
				"<dark_gray>discord.arcadeya.com"
		));
		errors.setAccountCreationMissing(List.of(
				"<green>ɪᴅᴇɴᴛɪᴄᴀ",
				"",
				"<white>Unable to create account.</white>",
				"<white>Required provider profile is missing.</white>",
				"",
				"<dark_gray>discord.arcadeya.com"
		));
		errors.setProviderLinkMissing(List.of(
				"<green>ɪᴅᴇɴᴛɪᴄᴀ",
				"",
				"<white>Unable to link provider.</white>",
				"<white>Required account data is missing.</white>",
				"",
				"<dark_gray>discord.arcadeya.com"
		));
		errors.setSessionBuildMissing(List.of(
				"<green>ɪᴅᴇɴᴛɪᴄᴀ",
				"",
				"<white>Unable to build session.</white>",
				"<white>Required context is missing.</white>",
				"",
				"<dark_gray>discord.arcadeya.com"
		));
		return errors;
	}

	private Messages.Connection.Journey buildJourneyMessages() {
		Messages.Connection.Journey journey = new Messages.Connection.Journey();
		Messages.Connection.Journey.Stage stage = new Messages.Connection.Journey.Stage();
		stage.setNoCompletion(List.of(
				"<green>ɪᴅᴇɴᴛɪᴄᴀ",
				"",
				"<white>Journey stage pipeline incomplete.</white>",
				"",
				"<dark_gray>discord.arcadeya.com"
		));

		Messages.Connection.Journey.Step step = new Messages.Connection.Journey.Step();
		step.setNoStatus(List.of(
				"<green>ɪᴅᴇɴᴛɪᴄᴀ",
				"",
				"<white>Journey step returned no status.</white>",
				"",
				"<dark_gray>discord.arcadeya.com"
		));
		Messages.Connection.Journey.Step.Enrollment enrollment = new Messages.Connection.Journey.Step.Enrollment();
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
		Messages.Connection.Journey.Step.Enrollment.EntryFormat enrollmentEntry = new Messages.Connection.Journey.Step.Enrollment.EntryFormat();
		enrollmentEntry.setFormat("   <dark_gray><click:run_command:/identica enroll {providerId}>▪ <gray>[{providerName}]:</gray> <white>{description}</click>");
		enrollmentEntry.setEmptyFormat("   <dark_gray><click:run_command:/identica enroll {providerId}>▪ <gray>[{providerName}]:</gray></click>");
		enrollment.setEntryFormat(enrollmentEntry);
		enrollment.setEmpty(List.of(
				"<green>ɪᴅᴇɴᴛɪᴄᴀ",
				"",
				"<white>No providers available for this account.</white>",
				"",
				"<dark_gray>discord.arcadeya.com"
		));
		enrollment.setDescriptions(Map.of(
				"premium", "Use Minecraft account for registration.",
				"cracked", "Register using password."
		));
		step.setEnrollment(enrollment);
		journey.setStage(stage);
		journey.setStep(step);
		return journey;
	}
}
