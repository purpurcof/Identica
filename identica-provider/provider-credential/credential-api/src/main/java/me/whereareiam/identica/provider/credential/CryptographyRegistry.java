package me.whereareiam.identica.provider.credential;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * Central registry for credential provider cryptography (password hashers).
 */
public interface CryptographyRegistry {
	@Nullable CryptographyAlgorithm resolve(@Nullable String id);

	@NotNull Collection<CryptographyAlgorithm> all();

	boolean register(@NotNull CryptographyAlgorithm hasher);

	boolean unregister(@NotNull String id);
}
