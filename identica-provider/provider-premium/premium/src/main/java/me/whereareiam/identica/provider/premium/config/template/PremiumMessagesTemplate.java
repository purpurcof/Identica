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
				"<gray>If you don't have a premium account, you will be",
				"<gray>forwarded to a fallback provider on your second attempt.",
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

		PremiumMessages.Commands commands = new PremiumMessages.Commands();
		PremiumMessages.Commands.Premium premium = new PremiumMessages.Commands.Premium();
		premium.setConfirm(List.of(
				" ",
				" <green><bold>Identica</bold>",
				" ",
				"  <white>Confirm premium migration</white>",
				"  <white>Use <gold>/premium confirm</gold> to continue</white>",
				"  <gray>Cancel with <red>/premium cancel</red></gray>",
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
