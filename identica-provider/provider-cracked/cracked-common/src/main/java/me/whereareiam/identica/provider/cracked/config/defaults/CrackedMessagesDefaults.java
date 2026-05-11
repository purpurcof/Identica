package me.whereareiam.identica.provider.cracked.config.defaults;

import com.google.inject.Singleton;
import me.whereareiam.configura.merge.MergeDefaultsProvider;
import me.whereareiam.identica.provider.cracked.config.CrackedMessages;

import java.util.List;

@Singleton
public class CrackedMessagesDefaults implements MergeDefaultsProvider<CrackedMessages> {
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
		CrackedMessages.Scenario.Registration.Status registrationStatus = new CrackedMessages.Scenario.Registration.Status();
		registrationStatus.setSuccess("{prefix}<white>You have been <green>successfully registered</green>.</white>");
		registrationStatus.setDisabled("{prefix}<white>Registration is <red>disabled</red>.</white>");
		registrationStatus.setAlreadyRegistered("{prefix}<white>Your account is already <green>registered</green>.</white>");
		registrationStatus.setMismatch("{prefix}<white>Passwords do not <red>match</red>.</white>");
		registrationStatus.setNoPending("{prefix}<white>No pending registration.</white>");
		registration.setStatus(registrationStatus);
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
		CrackedMessages.Scenario.Authentication.Status authenticationStatus = new CrackedMessages.Scenario.Authentication.Status();
		authenticationStatus.setSuccess("{prefix}<white>Successfully <green>logged in</green>.</white>");
		authenticationStatus.setInvalid("{prefix}<white>Invalid <red>password</red>.</white>");
		authenticationStatus.setNotRegistered("{prefix}<white>No cracked account found.</white>");
		authenticationStatus.setNoPending("{prefix}<white>No pending login.</white>");
		authentication.setStatus(authenticationStatus);

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
		CrackedMessages.Scenario.Authentication.Verification verification = new CrackedMessages.Scenario.Authentication.Verification();
		verification.setPrompt(List.of(
				" ",
				" <green><bold>Identica</bold>",
				" ",
				"  <white>Verification required for your account.</white>",
				"  <white>Use <yellow>/2fa confirm</yellow> <gray>[Code]</gray> to continue.</white>",
				" "
		));
		verification.setInvalid("{prefix}<white>Invalid verification <red>code</red>.</white>");
		verification.setRequired("{prefix}<white>A verification method is <red>required</red> before login.</white>");
		verification.setUnavailable("{prefix}<white>Your selected verification method is <red>unavailable</red>.</white>");
		authentication.setVerification(verification);
		scenario.setAuthentication(authentication);
		messages.setScenario(scenario);

		CrackedMessages.Completion completion = new CrackedMessages.Completion();
		CrackedMessages.Completion.Pipeline sessionCompletion = new CrackedMessages.Completion.Pipeline();
		CrackedMessages.Completion.Pipeline.Title sessionTitle = new CrackedMessages.Completion.Pipeline.Title();
		sessionTitle.setTitle("<gold><bold>Session Restored</bold></gold>");
		sessionTitle.setSubtitle("<dark_gray>Your session was reused.</dark_gray>");
		sessionCompletion.setTitle(sessionTitle);
		sessionCompletion.setBody(List.of(
				" ",
				" <green><bold>Identica</bold>",
				" ",
				"  <white>Welcome back, <green>{player}</green>.</white>",
				"  <white>Your existing <gold>session</gold> was reused.</white>",
				" ",
				"  <gray>Enjoy your stay.</gray>",
				" "
		));
		completion.setSession(sessionCompletion);

		CrackedMessages.Completion.Pipeline authenticationCompletion = new CrackedMessages.Completion.Pipeline();
		CrackedMessages.Completion.Pipeline.Title authenticationTitle = new CrackedMessages.Completion.Pipeline.Title();
		authenticationTitle.setTitle("<gold><bold>Signed In</bold></gold>");
		authenticationTitle.setSubtitle("<dark_gray>You were authenticated.</dark_gray>");
		authenticationCompletion.setTitle(authenticationTitle);
		authenticationCompletion.setBody(List.of(
				" ",
				" <green><bold>Identica</bold>",
				" ",
				"  <white>Welcome back, <green>{player}</green>.</white>",
				"  <white>You were authenticated <gold>via password</gold>.</white>",
				" ",
				"  <gray>Enjoy your stay.</gray>",
				" "
		));
		completion.setAuthentication(authenticationCompletion);

		CrackedMessages.Completion.Pipeline registrationCompletion = new CrackedMessages.Completion.Pipeline();
		CrackedMessages.Completion.Pipeline.Title registrationTitle = new CrackedMessages.Completion.Pipeline.Title();
		registrationTitle.setTitle("<gold><bold>Registered</bold></gold>");
		registrationTitle.setSubtitle("<dark_gray>You were registered.</dark_gray>");
		registrationCompletion.setTitle(registrationTitle);
		registrationCompletion.setBody(List.of(
				" ",
				" <green><bold>Identica</bold>",
				" ",
				"  <white>Welcome, <green>{player}</green>.</white>",
				"  <white>You just registered <gold>via cracked provider</gold>.</white>",
				"  <white>Please take a moment to read the server rules.</white>",
				" ",
				"  <gray>We hope you enjoy your stay.</gray>",
				" "
		));
		completion.setRegistration(registrationCompletion);

		CrackedMessages.Completion.Pipeline migrationCompletion = new CrackedMessages.Completion.Pipeline();
		CrackedMessages.Completion.Pipeline.Title migrationTitle = new CrackedMessages.Completion.Pipeline.Title();
		migrationTitle.setTitle("<gold><bold>Migrated</bold></gold>");
		migrationTitle.setSubtitle("<dark_gray>You were migrated to cracked.</dark_gray>");
		migrationCompletion.setTitle(migrationTitle);
		migrationCompletion.setBody(List.of(
				" ",
				" <green><bold>Identica</bold>",
				" ",
				"  <white>Welcome back, <green>{player}</green>.</white>",
				"  <white>Your account now authenticates <gold>via cracked provider</gold>.</white>",
				" ",
				"  <gray>You can continue using password login.</gray>",
				" "
		));
		completion.setMigration(migrationCompletion);
		messages.setCompletion(completion);

		CrackedMessages.Password password = new CrackedMessages.Password();
		password.setTooShort("{prefix}<white>Password is too <red>short</red>.</white>");
		password.setTooLong("{prefix}<white>Password is too <red>long</red>.</white>");
		password.setNoSpaces("{prefix}<white>Password cannot contain <red>spaces</red>.</white>");
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
				"  <white>You are about to switch to a <gold>cracked account</gold>.</white>",
				"  <white>After this, you will log in using a password.</white>",
				" ",
				"  <yellow>/cracked confirm</yellow> <dark_gray>- <white>Continue migration</white>",
				"  <yellow>/cracked cancel</yellow> <dark_gray>- <white>Cancel migration</white>",
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
		cracked.setVerificationRequired("{prefix}<white>Confirm your verification code with <yellow>/cracked confirm</yellow> <gray>[Code]</gray> before starting cracked migration.</white>");
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
		return messages;
	}
}
