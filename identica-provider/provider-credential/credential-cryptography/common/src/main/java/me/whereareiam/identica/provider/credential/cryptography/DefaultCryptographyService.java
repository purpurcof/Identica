package me.whereareiam.identica.provider.credential.cryptography;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.logging.Logger;
import me.whereareiam.identica.provider.credential.CryptographyAlgorithm;
import me.whereareiam.identica.provider.credential.CryptographyRegistry;
import me.whereareiam.identica.provider.credential.config.CredentialSettings;
import me.whereareiam.identica.provider.credential.event.account.password.PasswordVerifiedEvent;
import me.whereareiam.identica.provider.credential.model.CredentialAccount;
import me.whereareiam.identica.provider.credential.model.CryptographyOptions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class DefaultCryptographyService implements CryptographyService {
	private final CryptographyRegistry cryptographyRegistry;
	private final Provider<CredentialSettings> settingsProvider;
	private final EventManager eventManager;

	@Override
	public @Nullable PasswordCandidate hash(@NotNull String password) {
		CryptographyAlgorithm hasher = resolveConfiguredAlgorithm();
		if (hasher == null) return null;

		String passwordHash = hasher.hash(password, algorithmOptions());
		return new PasswordCandidate(passwordHash, hasher.id());
	}

	@Override
	public boolean verify(@NotNull CredentialAccount credential, @NotNull String password) {
		boolean matches = verify(password, credential.getPasswordHash(), credential.getHashingMethod());
		if (matches) eventManager.call(new PasswordVerifiedEvent(credential, password));

		return matches;
	}

	@Override
	public boolean verify(@NotNull String password, @NotNull String passwordHash) {
		CryptographyAlgorithm hasher = resolveConfiguredHasherOrThrow();
		return hasher.verify(password, passwordHash);
	}

	@Override
	public boolean verify(@NotNull String password, @NotNull String passwordHash, @NotNull String hashingMethod) {
		CryptographyAlgorithm hasher = resolveHasher(hashingMethod);
		if (hasher == null)return false;
		return hasher.verify(password, passwordHash);
	}

	@Override
	public @Nullable String primaryAlgorithm() {
		CredentialSettings.Cryptography cryptography = resolveCryptography();
		return cryptography != null ? cryptography.getAlgorithm() : null;
	}

	private CryptographyOptions algorithmOptions() {
		CredentialSettings.Cryptography cryptography = resolveCryptography();
		CryptographyOptions options = new CryptographyOptions();
		if (cryptography == null) return options;

		CredentialSettings.Cryptography.Algorithms algorithms = cryptography.getAlgorithms();
		if (algorithms == null) return options;

		CredentialSettings.Cryptography.Algorithms.Bcrypt bcrypt = algorithms.getBcrypt();
		if (bcrypt != null) options.setBcryptCost(bcrypt.getCost());

		CredentialSettings.Cryptography.Algorithms.Argon2 argon2 = algorithms.getArgon2();
		if (argon2 != null) {
			options.setArgon2Iterations(argon2.getIterations());
			options.setArgon2MemoryKb(argon2.getMemoryKb());
			options.setArgon2Parallelism(argon2.getParallelism());
		}

		return options;
	}

	private @Nullable CryptographyAlgorithm resolveConfiguredAlgorithm() {
		String algorithm = primaryAlgorithm();
		if (algorithm == null || algorithm.isBlank()) {
			Logger.warn("Credential hashing algorithm is not configured");
			return null;
		}

		CryptographyAlgorithm hasher = resolveHasher(algorithm);
		if (hasher == null) Logger.warn("Credential hasher %s is missing", algorithm);

		return hasher;
	}

	private @NotNull CryptographyAlgorithm resolveConfiguredHasherOrThrow() {
		String algorithm = primaryAlgorithm();
		if (algorithm == null || algorithm.isBlank())
			throw new IllegalStateException("Credential hashing algorithm is not configured");

		CryptographyAlgorithm hasher = resolveHasher(algorithm);
		if (hasher == null) throw new IllegalStateException(String.format("Credential hasher %s is missing", algorithm));
		return hasher;
	}

	private @Nullable CryptographyAlgorithm resolveHasher(@Nullable String id) {
		return cryptographyRegistry.resolve(id);
	}

	private @Nullable CredentialSettings.Cryptography resolveCryptography() {
		CredentialSettings settings = settingsProvider.get();
		return settings != null ? settings.getCryptography() : null;
	}
}
