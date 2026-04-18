package me.whereareiam.identica.provider.premium.config.template;

import com.google.inject.Singleton;
import me.whereareiam.configura.TemplateProvider;
import me.whereareiam.identica.provider.premium.config.PremiumMessages;

import java.util.List;

@Singleton
public class PremiumMessagesTemplate implements TemplateProvider<PremiumMessages> {
	@Override
	public PremiumMessages supply(PremiumMessages messages) {
		PremiumMessages.Verification verification = new PremiumMessages.Verification();
		verification.setRejoin(List.of(
				"<green>ɪᴅᴇɴᴛɪᴄᴀ",
				"",
				"<white>Please rejoin to verify your premium account.",
				"",
				"<gray>If you don't have a premium account, you'll be",
				"<gray>asked to choose a different provider next time.",
				"",
				"<dark_gray>discord.arcadeya.com"
		));
		verification.setInvalidSession(List.of(
				"<green>ɪᴅᴇɴᴛɪᴄᴀ",
				"",
				"<white>Invalid Mojang session.",
				"<white>Rejoin with your premium account",
				"",
				"<dark_gray>discord.arcadeya.com"
		));
		messages.setVerification(verification);

		PremiumMessages.Completion completion = new PremiumMessages.Completion();
		PremiumMessages.Completion.Pipeline authenticationCompletion = new PremiumMessages.Completion.Pipeline();
		PremiumMessages.Completion.Pipeline.Title authenticationTitle = new PremiumMessages.Completion.Pipeline.Title();
		authenticationTitle.setTitle("<gold><bold>Verified</bold></gold>");
		authenticationTitle.setSubtitle("<dark_gray>You were authenticated.</dark_gray>");
		authenticationCompletion.setTitle(authenticationTitle);
		authenticationCompletion.setBody(List.of(
				" ",
				" <green><bold>Identica</bold>",
				" ",
				"  <white>Welcome back, <green>{player}</green>.</white>",
				"  <white>You were authenticated <gold>via premium provider</gold>.</white>",
				" ",
				"  <gray>Enjoy your session.</gray>",
				" "
		));
		completion.setAuthentication(authenticationCompletion);

		PremiumMessages.Completion.Pipeline migrationCompletion = new PremiumMessages.Completion.Pipeline();
		PremiumMessages.Completion.Pipeline.Title migrationTitle = new PremiumMessages.Completion.Pipeline.Title();
		migrationTitle.setTitle("<gold><bold>Migrated</bold></gold>");
		migrationTitle.setSubtitle("<dark_gray>You were migrated to premium.</dark_gray>");
		migrationCompletion.setTitle(migrationTitle);
		migrationCompletion.setBody(List.of(
				" ",
				" <green><bold>Identica</bold>",
				" ",
				"  <white>Welcome back, <green>{player}</green>.</white>",
				"  <white>Your account now authenticates <gold>via premium provider</gold>.</white>",
				" ",
				"  <gray>Rejoin with your licensed Minecraft account from now on.</gray>",
				" "
		));
		completion.setMigration(migrationCompletion);
		messages.setCompletion(completion);

		PremiumMessages.Commands commands = new PremiumMessages.Commands();
		PremiumMessages.Commands.Premium premium = new PremiumMessages.Commands.Premium();
		premium.setConfirm(List.of(
				" ",
				" <green><bold>Identica</bold>",
				"  <white>You are about to switch to a <gold>licensed Minecraft account</gold>.</white>",
				"  <white>After this, you will log in with that licensed account.</white>",
				" ",
				"  <yellow>/premium confirm</yellow> <dark_gray>- <white>Continue migration</white>",
				"  <yellow>/premium cancel</yellow> <dark_gray>- <white>Cancel migration</white>",
				" "
		));
		premium.setConfirmed(List.of(
				"<green>ɪᴅᴇɴᴛɪᴄᴀ",
				"",
				"<white>Please rejoin the server to proceed with</white>",
				"<white>migration to the premium provider.</white>",
				"",
				"<dark_gray>discord.arcadeya.com"
		));
		premium.setCancelled("{prefix}<white>Premium migration cancelled.</white>");
		premium.setExpired("{prefix}<white>Premium migration request expired.</white>");
		premium.setNoPending("{prefix}<white>No pending premium migration.</white>");
		premium.setPendingExists("{prefix}<white>Premium migration already pending.</white>");
		premium.setAlreadyPrimary("{prefix}<white>Premium is already your primary provider.</white>");
		commands.setPremium(premium);
		messages.setCommands(commands);

		return messages;
	}
}
