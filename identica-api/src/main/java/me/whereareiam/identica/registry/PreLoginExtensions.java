package me.whereareiam.identica.registry;

import me.whereareiam.identica.Key;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Registry for pre-login extension data keyed by username.
 * This data is short-lived and intended for providers that need to
 * observe platform information before a connection or identity exists.
 */
public interface PreLoginExtensions {
	/**
	 * Stores a value for the provided username and key.
	 *
	 * @param username username to associate
	 * @param key key describing the stored value
	 * @param value value to store
	 * @param ttlMs time-to-live in milliseconds
	 * @param <T> value type
	 */
	<T> void put(@NotNull String username, @NotNull Key<T> key, @NotNull T value, long ttlMs);

	/**
	 * Returns a stored value for the provided username and key without clearing it.
	 *
	 * @param username username to query
	 * @param key key describing the stored value
	 * @param <T> value type
	 * @return optional containing the value when present
	 */
	@NotNull <T> Optional<T> peek(@NotNull String username, @NotNull Key<T> key);

	/**
	 * Returns a stored value for the provided username and key, clearing it after read.
	 *
	 * @param username username to query
	 * @param key key describing the stored value
	 * @param <T> value type
	 * @return optional containing the value when present
	 */
	@NotNull <T> Optional<T> consume(@NotNull String username, @NotNull Key<T> key);

	/**
	 * Clears all stored values for the provided username.
	 *
	 * @param username username to clear
	 */
	void clear(@NotNull String username);
}
