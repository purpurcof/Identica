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
		verification.setPrompt("{prefix}<yellow>Join with premium? Use /premium to verify or /cracked to continue cracked.</yellow>");
		verification.setInvalidSession(List.of(
				"<gold>ɪᴅᴇɴᴛɪᴄᴀ",
				"",
				"<white>Invalid Mojang session.",
				"<white>Rejoin with your premium account",
				"",
				"<gray>discord.arcadeya.com"
		));
		messages.setVerification(verification);

		PremiumMessages.Commands commands = new PremiumMessages.Commands();
		PremiumMessages.Commands.Premium premium = new PremiumMessages.Commands.Premium();
		premium.setConfirmed(List.of(
				"{prefix}<green>Premium verification started.</green>"
		));
		commands.setPremium(premium);
		messages.setCommands(commands);

		return messages;
	}
}
