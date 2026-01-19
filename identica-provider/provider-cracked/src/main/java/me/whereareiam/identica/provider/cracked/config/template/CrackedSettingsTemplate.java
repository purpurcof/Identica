package me.whereareiam.identica.provider.cracked.config.template;

import com.google.inject.Singleton;
import me.whereareiam.configura.TemplateProvider;
import me.whereareiam.identica.provider.cracked.config.CrackedSettings;

@Singleton
public class CrackedSettingsTemplate implements TemplateProvider<CrackedSettings> {
	@Override
	public CrackedSettings supply(CrackedSettings config) {
		CrackedSettings.Register register = new CrackedSettings.Register();
		register.setEnabled(true);
		register.setRequireRepeat(true);
		config.setRegister(register);

		CrackedSettings.Login login = new CrackedSettings.Login();
		login.setMaxAttempts(5);
		login.setLockSeconds(300);
		CrackedSettings.Session session = new CrackedSettings.Session();
		session.setAutoLoginSeconds(1209600L);
		login.setSession(session);
		config.setLogin(login);

		CrackedSettings.Username username = new CrackedSettings.Username();
		username.setMinLength(3);
		username.setMaxLength(16);
		username.setAllowedPattern("^[a-zA-Z0-9_]+$");

		CrackedSettings.Requirements requirements = new CrackedSettings.Requirements();
		requirements.setMinLength(6);
		requirements.setMaxLength(64);
		requirements.setMinUpper(0);
		requirements.setMinLower(1);
		requirements.setMinNumber(1);
		requirements.setMinSpecial(0);

		CrackedSettings.Hashing hashing = new CrackedSettings.Hashing();
		hashing.setAlgorithm("argon2");
		hashing.setRehashOnLogin(true);

		CrackedSettings.Password password = new CrackedSettings.Password();
		password.setRequirements(requirements);
		password.setHashing(hashing);

		CrackedSettings.Credentials credentials = new CrackedSettings.Credentials();
		credentials.setUsername(username);
		credentials.setPassword(password);
		config.setCredentials(credentials);

		CrackedSettings.Conflict conflict = new CrackedSettings.Conflict();
		conflict.setPrefix("CR_");
		conflict.setSuffix("");
		conflict.setApplyOnConflict(true);
		config.setConflict(conflict);

		return config;
	}
}
