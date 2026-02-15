package me.whereareiam.identica.provider.cracked.cryptography.argon2id;

import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;
import me.whereareiam.identica.provider.cracked.model.CryptographyOptions;
import me.whereareiam.identica.provider.cracked.CryptographyAlgorithm;
import org.jetbrains.annotations.NotNull;

public class Argon2IdCryptographyAlgorithm implements CryptographyAlgorithm {
	private final Argon2 argon2 = Argon2Factory.create(Argon2Factory.Argon2Types.ARGON2id);

	@Override
	public @NotNull String id() {
		return "argon2id";
	}

	@Override
	public @NotNull String hash(@NotNull String password, @NotNull CryptographyOptions options) {
		int iterations = options.getArgon2Iterations();
		int memoryKb = options.getArgon2MemoryKb();
		int parallelism = options.getArgon2Parallelism();
		if (iterations <= 0)
			throw new IllegalArgumentException("argon2id iterations must be positive");
		if (memoryKb <= 0)
			throw new IllegalArgumentException("argon2id memoryKb must be positive");
		if (parallelism <= 0)
			throw new IllegalArgumentException("argon2id parallelism must be positive");

		return argon2.hash(iterations, memoryKb, parallelism, password.toCharArray());
	}

	@Override
	public boolean verify(@NotNull String password, @NotNull String hash) {
		return argon2.verify(hash, password.toCharArray());
	}
}
