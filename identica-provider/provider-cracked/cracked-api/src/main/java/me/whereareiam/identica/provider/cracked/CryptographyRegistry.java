package me.whereareiam.identica.provider.cracked;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * Central registry for cracked provider cryptography (password hashers).
 */
public interface CryptographyRegistry {
	@Nullable CryptographyAlgorithm resolve(@Nullable String id);

	@NotNull Collection<CryptographyAlgorithm> all();

	boolean register(@NotNull CryptographyAlgorithm hasher);

	boolean unregister(@NotNull String id);
}
