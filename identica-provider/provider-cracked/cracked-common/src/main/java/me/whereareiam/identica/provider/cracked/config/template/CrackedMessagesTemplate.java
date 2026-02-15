package me.whereareiam.identica.provider.cracked.config.template;

import com.google.inject.Singleton;
import me.whereareiam.configura.TemplateProvider;
import me.whereareiam.identica.provider.cracked.config.CrackedMessages;

import java.util.List;

@Singleton
public class CrackedMessagesTemplate implements TemplateProvider<CrackedMessages> {
	@Override
	public CrackedMessages supply(CrackedMessages messages) {
		CrackedMessages.Register register = new CrackedMessages.Register();
		register.setPrompt(List.of(
				" ",
				" <green><bold>Identica</bold>",
				" ",
				"  <white>Create your cracked account</white>",
				"  <white>Use <gold>/pass <password></gold> to continue</white>",
				" "
		));
		register.setConfirmPrompt(List.of(
				" ",
				" <green><bold>Identica</bold>",
				" ",
				"  <white>Confirm your password</white>",
				"  <white>Use <gold>/passconfirm <repeat></gold> to continue</white>",
				" "
		));
		register.setSuccess("{prefix}<white>Your account was <green>registered</green>.</white>");
		register.setDisabled("{prefix}<white>Registration is <red>disabled</red>.</white>");
		register.setAlreadyRegistered("{prefix}<white>Your account is already <green>registered</green>.</white>");
		register.setMismatch("{prefix}<white>Passwords do not <red>match</red>.</white>");
		register.setNoPending("{prefix}<white>No pending registration.</white>");
		messages.setRegister(register);

		CrackedMessages.Login login = new CrackedMessages.Login();
		login.setPrompt(List.of(
				" ",
				" <green><bold>Identica</bold>",
				" ",
				"  <white>Login required</white>",
				"  <white>Use <gold>/login <password></gold> to continue</white>",
				" "
		));
		login.setSuccess("{prefix}<white>Successfully <green>logged in</green>.</white>");
		login.setInvalid("{prefix}<white>Invalid <red>password</red>.</white>");
		login.setNotRegistered("{prefix}<white>No cracked account found.</white>");
		login.setNoPending("{prefix}<white>No pending login.</white>");
		messages.setLogin(login);

		CrackedMessages.Password password = new CrackedMessages.Password();
		password.setTooShort("{prefix}<white>Password is too <red>short</red>.</white>");
		password.setTooLong("{prefix}<white>Password is too <red>long</red>.</white>");
		password.setMissingUpper("{prefix}<white>Password needs an <red>uppercase</red> letter.</white>");
		password.setMissingLower("{prefix}<white>Password needs a <red>lowercase</red> letter.</white>");
		password.setMissingNumber("{prefix}<white>Password needs a <red>number</red>.</white>");
		password.setMissingSpecial("{prefix}<white>Password needs a <red>special</red> character.</white>");
		messages.setPassword(password);

		CrackedMessages.Lockout lockout = new CrackedMessages.Lockout();
		lockout.setExceeded(List.of(
				"<green>ɪᴅᴇɴᴛɪᴄᴀ",
				"",
				"<white>Too many attempts.</white>",
				"<white>Try again in <red>{seconds}s</red>.</white>",
				"",
				"<dark_gray>discord.arcadeya.com"
		));
		messages.setLockout(lockout);

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
				"  <white>Use <gold>/cracked confirm</gold> to continue</white>",
				"  <gray>Cancel with <red>/cracked cancel</red></gray>",
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
