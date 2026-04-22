package me.whereareiam.identica.provider.cracked.config.template;

import com.google.inject.Singleton;
import me.whereareiam.configura.TemplateProvider;
import me.whereareiam.identica.provider.cracked.config.CrackedSettings;

import java.time.Duration;

@Singleton
public class CrackedSettingsTemplate implements TemplateProvider<CrackedSettings> {
	@Override
	public CrackedSettings supply(CrackedSettings config) {
		CrackedSettings.Scenario.Registration registration = new CrackedSettings.Scenario.Registration();
		registration.setEnabled(true);
		registration.setRequireRepeat(true);

		CrackedSettings.Scenario.Authentication authentication = new CrackedSettings.Scenario.Authentication();
		CrackedSettings.Scenario.Authentication.Bruteforce bruteforce = new CrackedSettings.Scenario.Authentication.Bruteforce();
		bruteforce.setMaxAttempts(5);
		CrackedSettings.Scenario.Authentication.Bruteforce.Lockout lockout =
				new CrackedSettings.Scenario.Authentication.Bruteforce.Lockout();
		lockout.setEnabled(true);
		lockout.setDuration(Duration.ofSeconds(300));
		bruteforce.setLockout(lockout);
		CrackedSettings.Scenario.Authentication.Bruteforce.Warning warning =
				new CrackedSettings.Scenario.Authentication.Bruteforce.Warning();
		warning.setEnabled(true);
		warning.setThresholdPercentage(50);
		bruteforce.setWarning(warning);
		authentication.setBruteforce(bruteforce);

		CrackedSettings.Scenario scenario = new CrackedSettings.Scenario();
		scenario.setRegistration(registration);
		scenario.setAuthentication(authentication);
		CrackedSettings.Scenario.ChangePassword changePassword = new CrackedSettings.Scenario.ChangePassword();
		changePassword.setRequireRepeat(true);
		scenario.setChangePassword(changePassword);
		config.setScenario(scenario);

		CrackedSettings.Scenario.Registration.Username username = new CrackedSettings.Scenario.Registration.Username();
		username.setMinLength(3);
		username.setMaxLength(16);
		username.setPattern("^[a-zA-Z0-9_]+$");

		CrackedSettings.Scenario.Registration.Password password = new CrackedSettings.Scenario.Registration.Password();
		password.setMinLength(6);
		password.setMaxLength(32);
		password.setMinUpper(1);
		password.setMinLower(1);
		password.setMinNumber(1);
		password.setMinSpecial(1);
		registration.setUsername(username);
		registration.setPassword(password);

		CrackedSettings.Cryptography cryptography = new CrackedSettings.Cryptography();
		cryptography.setAlgorithm("bcrypt");
		cryptography.setAutoupgrade(true);

		CrackedSettings.Cryptography.Algorithms algorithms = new CrackedSettings.Cryptography.Algorithms();

		CrackedSettings.Cryptography.Algorithms.Bcrypt bcrypt = new CrackedSettings.Cryptography.Algorithms.Bcrypt();
		bcrypt.setCost(12);
		algorithms.setBcrypt(bcrypt);

		CrackedSettings.Cryptography.Algorithms.Argon2 argon2 = new CrackedSettings.Cryptography.Algorithms.Argon2();
		argon2.setIterations(3);
		argon2.setParallelism(1);
		argon2.setMemoryKb(65536);
		algorithms.setArgon2(argon2);

		cryptography.setAlgorithms(algorithms);

		config.setCryptography(cryptography);

		CrackedSettings.Replication.Cache cache = new CrackedSettings.Replication.Cache();
		cache.setLockout("cracked-lockout");
		CrackedSettings.Replication replication = new CrackedSettings.Replication();
		replication.setCache(cache);
		config.setReplication(replication);

		return config;
	}
}
