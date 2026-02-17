package me.whereareiam.identica.provider.cracked.config.template;

import com.google.inject.Singleton;
import me.whereareiam.configura.TemplateProvider;
import me.whereareiam.identica.provider.cracked.config.CrackedMessages;

import java.util.List;

@Singleton
public class CrackedMessagesTemplate implements TemplateProvider<CrackedMessages> {
	@Override
	public CrackedMessages supply(CrackedMessages messages) {
		CrackedMessages.Scenario scenario = new CrackedMessages.Scenario();

		CrackedMessages.Scenario.Registration registration = new CrackedMessages.Scenario.Registration();
		registration.setPrompt(List.of(
				" ",
				" <green><bold>Identica</bold>",
				" ",
				"  <white>To create your cracked account, you have to</white>",
				"  <white>walk through some registration steps.</white>",
				" ",
				"  <gray>Information:",
				"   <gray>6-32 characters, at least 1 uppercase, 1 lowercase,</gray>",
				"   <gray>1 number, and 1 special character.</gray>",
				" ", 
				"  <white>Use <yellow>/pass</yellow> <gray>[Password]</gray> to continue.</white>",
				" "
		));
		registration.setConfirmPrompt(List.of(
				" ",
				" <green><bold>Identica</bold>",
				" ",
				"  <white>To finish your cracked account registration,</white>",
				"  <white>repeat the same password you entered before.</white>",
				" ",
				"  <white>Use <yellow>/passconfirm</yellow> <gray>[Password]</gray> to continue.</white>",
				" "
		));
		registration.setSuccess("{prefix}<white>You have been <green>successfully registered</green>.</white>");
		registration.setDisabled("{prefix}<white>Registration is <red>disabled</red>.</white>");
		registration.setAlreadyRegistered("{prefix}<white>Your account is already <green>registered</green>.</white>");
		registration.setMismatch("{prefix}<white>Passwords do not <red>match</red>.</white>");
		registration.setNoPending("{prefix}<white>No pending registration.</white>");
		scenario.setRegistration(registration);

		CrackedMessages.Scenario.Authentication authentication = new CrackedMessages.Scenario.Authentication();
		authentication.setPrompt(List.of(
				" ",
				" <green><bold>Identica</bold>",
				" ",
				"  <white>Welcome back to our server.</white>",
				"  <white>Please log in to proceed.</white>",
				" ",
				"  <white>Use <yellow>/login</yellow> <gray>[Password]</gray> to continue.</white>",
				" "
		));
		authentication.setSuccess("{prefix}<white>Successfully <green>logged in</green>.</white>");
		authentication.setInvalid("{prefix}<white>Invalid <red>password</red>.</white>");
		authentication.setNotRegistered("{prefix}<white>No cracked account found.</white>");
		authentication.setNoPending("{prefix}<white>No pending login.</white>");

		CrackedMessages.Scenario.Authentication.Bruteforce bruteforce = new CrackedMessages.Scenario.Authentication.Bruteforce();
		bruteforce.setExceeded(List.of(
				"<green>ɪᴅᴇɴᴛɪᴄᴀ",
				"",
				"<white>Too many attempts.</white>",
				"<white>Try again in <red>{seconds}s</red>.</white>",
				"",
				"<dark_gray>discord.arcadeya.com"
		));
		bruteforce.setRemaining(List.of(
				"{prefix}<white>You have <red>{remaining}</red> tries left.</white>"
		));
		authentication.setBruteforce(bruteforce);
		scenario.setAuthentication(authentication);
		messages.setScenario(scenario);

		CrackedMessages.Password password = new CrackedMessages.Password();
		password.setTooShort("{prefix}<white>Password is too <red>short</red>.</white>");
		password.setTooLong("{prefix}<white>Password is too <red>long</red>.</white>");
		password.setMissingUpper("{prefix}<white>Password needs an <red>uppercase</red> letter.</white>");
		password.setMissingLower("{prefix}<white>Password needs a <red>lowercase</red> letter.</white>");
		password.setMissingNumber("{prefix}<white>Password needs a <red>number</red>.</white>");
		password.setMissingSpecial("{prefix}<white>Password needs a <red>special</red> character.</white>");
		messages.setPassword(password);

		CrackedMessages.ChangePassword changePassword = new CrackedMessages.ChangePassword();
		changePassword.setSuccess("{prefix}<white>Password <green>updated</green>.</white>");
		changePassword.setMismatch("{prefix}<white>Passwords do not <red>match</red>.</white>");
		changePassword.setInvalidCurrent("{prefix}<white>Current password is <red>invalid</red>.</white>");
		changePassword.setNotLoggedIn("{prefix}<white>You must be <red>logged in</red> to change password.</white>");
		messages.setChangePassword(changePassword);

		CrackedMessages.Commands commands = new CrackedMessages.Commands();
		CrackedMessages.Commands.Cracked cracked = new CrackedMessages.Commands.Cracked();
		cracked.setConfirm(List.of(
				" ",
				" <green><bold>Identica</bold>",
				" ",
				"  <white>Confirm cracked migration</white>",
				"  <white>Use <yellow>/cracked</yellow> <gray>[Confirm]</gray> to continue</white>",
				"  <white>Cancel with <yellow>/cracked</yellow> <gray>[Cancel]</gray></white>",
				" "
		));
		cracked.setConfirmed(List.of(
				"<green>ɪᴅᴇɴᴛɪᴄᴀ",
				"",
				"<white>Please rejoin the server to proceed with</white>",
				"<white>migration to the cracked provider.</white>",
				"",
				"<dark_gray>discord.arcadeya.com"
		));
		cracked.setCancelled("{prefix}<white>Cracked migration cancelled.</white>");
		cracked.setExpired("{prefix}<white>Cracked migration request expired.</white>");
		cracked.setNoPending("{prefix}<white>No pending cracked migration.</white>");
		cracked.setPendingExists("{prefix}<white>Cracked migration already pending.</white>");
		cracked.setAlreadyPrimary("{prefix}<white>Cracked is already your primary provider.</white>");
		commands.setCracked(cracked);

		CrackedMessages.Commands.Admin admin = new CrackedMessages.Commands.Admin();
		admin.setRegistered("{prefix}<white>Cracked account <green>registered</green>.</white>");
		admin.setDeleted("{prefix}<white>Cracked account <green>deleted</green>.</white>");
		admin.setPasswordSet("{prefix}<white>Password <green>updated</green>.</white>");
		admin.setNotFound("{prefix}<white>No cracked account found.</white>");
		admin.setAlreadyRegistered("{prefix}<white>Cracked account already <green>registered</green>.</white>");
		commands.setAdmin(admin);

		messages.setCommands(commands);

		CrackedMessages.Conflict conflict = new CrackedMessages.Conflict();
		conflict.setRenamed(List.of(
				" ",
				" <green><bold>Identica</bold>",
				" ",
				"  <white>Using <green>{username}</green> for this session.</white>",
				" "
		));
		conflict.setDenied(List.of(
				"<white>Name conflict detected. Access denied.</white>",
				"<white>You were kicked due to a name conflict.</white>",
				"",
				"<dark_gray>discord.arcadeya.com"
		));
		messages.setConflict(conflict);
		return messages;
	}
}
