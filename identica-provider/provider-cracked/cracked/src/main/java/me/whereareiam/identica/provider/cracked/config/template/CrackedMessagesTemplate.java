package me.whereareiam.identica.provider.cracked.config.template;

import com.google.inject.Singleton;
import me.whereareiam.configura.TemplateProvider;
import me.whereareiam.identica.provider.cracked.config.CrackedMessages;

import java.util.List;

@Singleton
public class CrackedMessagesTemplate implements TemplateProvider<CrackedMessages> {
	@Override
	public CrackedMessages supply(CrackedMessages messages) {
		messages.setPrefix("<gold>Identica</gold> <dark_gray>| </dark_gray>");

		CrackedMessages.Register register = new CrackedMessages.Register();
		register.setPrompt("{prefix}<yellow>Register with /register <password> <repeat></yellow>");
		register.setSuccess("{prefix}<green>Registered.</green>");
		register.setDisabled("{prefix}<red>Registration is disabled.</red>");
		register.setAlreadyRegistered("{prefix}<red>You are already registered.</red>");
		register.setMismatch("{prefix}<red>Passwords do not match.</red>");
		messages.setRegister(register);

		CrackedMessages.Login login = new CrackedMessages.Login();
		login.setPrompt("{prefix}<yellow>Login with /login <password></yellow>");
		login.setSuccess("{prefix}<green>Logged in.</green>");
		login.setInvalid("{prefix}<red>Invalid password.</red>");
		login.setNotRegistered("{prefix}<red>You are not registered.</red>");
		messages.setLogin(login);

		CrackedMessages.Password password = new CrackedMessages.Password();
		password.setTooShort("{prefix}<red>Password is too short.</red>");
		password.setTooLong("{prefix}<red>Password is too long.</red>");
		password.setMissingUpper("{prefix}<red>Password needs an uppercase letter.</red>");
		password.setMissingLower("{prefix}<red>Password needs a lowercase letter.</red>");
		password.setMissingNumber("{prefix}<red>Password needs a number.</red>");
		password.setMissingSpecial("{prefix}<red>Password needs a special character.</red>");
		messages.setPassword(password);

		CrackedMessages.Lockout lockout = new CrackedMessages.Lockout();
		lockout.setExceeded("{prefix}<red>Too many attempts. Try again in {seconds}s.</red>");
		messages.setLockout(lockout);

		CrackedMessages.Conflict conflict = new CrackedMessages.Conflict();
		conflict.setRenamed(List.of(
				" ",
				"<gold> <bold>Identica</bold>",
				"<white>  Using <green>{username}</green> for this session.</white>",
				" "
		));
		conflict.setDenied(List.of(
				"<red>Name conflict detected. Access denied.</red>",
				"<red>You were kicked due to a name conflict.</red>"
		));
		messages.setConflict(conflict);
		return messages;
	}
}
