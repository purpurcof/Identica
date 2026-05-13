package me.whereareiam.identica.provider.credential.config.defaults;

import com.google.inject.Singleton;
import me.whereareiam.configura.merge.defaults.MergeDefaultsProvider;
import me.whereareiam.identica.provider.credential.config.CredentialSettings;

import java.time.Duration;

@Singleton
public class CredentialSettingsDefaults implements MergeDefaultsProvider<CredentialSettings> {
	@Override
	public CredentialSettings supply(CredentialSettings config) {
		CredentialSettings.Scenario.Registration registration = new CredentialSettings.Scenario.Registration();
		registration.setEnabled(true);
		registration.setRequireRepeat(true);

		CredentialSettings.Scenario.Authentication authentication = new CredentialSettings.Scenario.Authentication();
		CredentialSettings.Scenario.Authentication.Bruteforce bruteforce = new CredentialSettings.Scenario.Authentication.Bruteforce();
		bruteforce.setMaxAttempts(5);
		CredentialSettings.Scenario.Authentication.Bruteforce.Lockout lockout =
				new CredentialSettings.Scenario.Authentication.Bruteforce.Lockout();
		lockout.setEnabled(true);
		lockout.setDuration(Duration.ofSeconds(300));
		bruteforce.setLockout(lockout);
		CredentialSettings.Scenario.Authentication.Bruteforce.Warning warning =
				new CredentialSettings.Scenario.Authentication.Bruteforce.Warning();
		warning.setEnabled(true);
		warning.setThresholdPercentage(50);
		bruteforce.setWarning(warning);
		authentication.setBruteforce(bruteforce);

		CredentialSettings.Scenario scenario = new CredentialSettings.Scenario();
		scenario.setRegistration(registration);
		scenario.setAuthentication(authentication);
		CredentialSettings.Scenario.ChangePassword changePassword = new CredentialSettings.Scenario.ChangePassword();
		changePassword.setRequireRepeat(true);
		scenario.setChangePassword(changePassword);
		config.setScenario(scenario);

		CredentialSettings.Scenario.Registration.Username username = new CredentialSettings.Scenario.Registration.Username();
		username.setMinLength(3);
		username.setMaxLength(16);
		username.setPattern("^[a-zA-Z0-9_]+$");

		CredentialSettings.Scenario.Registration.Password password = new CredentialSettings.Scenario.Registration.Password();
		password.setMinLength(6);
		password.setMaxLength(32);
		password.setMinUpper(1);
		password.setMinLower(1);
		password.setMinNumber(1);
		password.setMinSpecial(1);
		registration.setUsername(username);
		registration.setPassword(password);

		CredentialSettings.Cryptography cryptography = new CredentialSettings.Cryptography();
		cryptography.setAlgorithm("bcrypt");
		cryptography.setAutoupgrade(true);

		CredentialSettings.Cryptography.Algorithms algorithms = new CredentialSettings.Cryptography.Algorithms();

		CredentialSettings.Cryptography.Algorithms.Bcrypt bcrypt = new CredentialSettings.Cryptography.Algorithms.Bcrypt();
		bcrypt.setCost(12);
		algorithms.setBcrypt(bcrypt);

		CredentialSettings.Cryptography.Algorithms.Argon2 argon2 = new CredentialSettings.Cryptography.Algorithms.Argon2();
		argon2.setIterations(3);
		argon2.setParallelism(1);
		argon2.setMemoryKb(65536);
		algorithms.setArgon2(argon2);

		cryptography.setAlgorithms(algorithms);

		config.setCryptography(cryptography);

		CredentialSettings.Replication.Cache cache = new CredentialSettings.Replication.Cache();
		cache.setLockout("password-lockout");
		CredentialSettings.Replication replication = new CredentialSettings.Replication();
		replication.setCache(cache);
		config.setReplication(replication);

		return config;
	}
}
