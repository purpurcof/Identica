package me.whereareiam.identica.provider.cracked.config.template;

import com.google.inject.Singleton;
import me.whereareiam.configura.TemplateProvider;
import me.whereareiam.identica.provider.cracked.config.CrackedSettings;

@Singleton
public class CrackedSettingsTemplate implements TemplateProvider<CrackedSettings> {
	@Override
	public CrackedSettings supply(CrackedSettings config) {
		CrackedSettings.Registration registration = new CrackedSettings.Registration();
		registration.setEnabled(true);
		registration.setRequireRepeat(true);

		CrackedSettings.Authentication authentication = new CrackedSettings.Authentication();
		authentication.setMaxAttempts(5);
		authentication.setLockSeconds(300);
		CrackedSettings.Session session = new CrackedSettings.Session();
		session.setAutoLoginSeconds(1209600L);
		session.setRequireSameIp(true);
		authentication.setSession(session);

		CrackedSettings.Scenario scenario = new CrackedSettings.Scenario();
		scenario.setRegistration(registration);
		scenario.setAuthentication(authentication);
		config.setScenario(scenario);

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

		CrackedSettings.Password password = new CrackedSettings.Password();
		password.setRequirements(requirements);
		registration.setUsername(username);
		registration.setPassword(password);

		CrackedSettings.Cryptography cryptography = new CrackedSettings.Cryptography();
		cryptography.setAlgorithm("argon2");
		cryptography.setAutoupgrade(true);

		CrackedSettings.Algorithms algorithms = new CrackedSettings.Algorithms();

		CrackedSettings.Bcrypt bcrypt = new CrackedSettings.Bcrypt();
		bcrypt.setCost(12);
		algorithms.setBcrypt(bcrypt);

		CrackedSettings.Argon2 argon2 = new CrackedSettings.Argon2();
		argon2.setIterations(3);
		argon2.setParallelism(1);
		argon2.setMemoryKb(65536);
		algorithms.setArgon2(argon2);

		cryptography.setAlgorithms(algorithms);

		config.setCryptography(cryptography);

		CrackedSettings.Cache cache = new CrackedSettings.Cache();
		cache.setLockout("cracked-lockout");
		CrackedSettings.Replication replication = new CrackedSettings.Replication();
		replication.setCache(cache);
		config.setReplication(replication);

		return config;
	}
}
