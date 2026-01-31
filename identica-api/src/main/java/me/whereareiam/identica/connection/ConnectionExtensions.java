package me.whereareiam.identica.connection;

import me.whereareiam.identica.Key;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;

/**
 * Registry for connection-scoped extension data keyed by connection id.
 */
public interface ConnectionExtensions {
	/**
	 * Stores a value for the provided connection id and key.
	 *
	 * @param connectionUniqueId connection id
	 * @param key key describing the stored value
	 * @param value value to store
	 * @param <T> value type
	 */
	<T> void put(@NotNull UUID connectionUniqueId, @NotNull Key<T> key, @NotNull T value);

	/**
	 * Returns a stored value for the provided connection id and key.
	 *
	 * @param connectionUniqueId connection id
	 * @param key key describing the stored value
	 * @param <T> value type
	 * @return optional containing the value when present
	 */
	@NotNull <T> Optional<T> get(@NotNull UUID connectionUniqueId, @NotNull Key<T> key);

	/**
	 * Removes a stored value for the provided connection id and key.
	 *
	 * @param connectionUniqueId connection id
	 * @param key key describing the stored value
	 * @param <T> value type
	 * @return optional containing the removed value when present
	 */
	@NotNull <T> Optional<T> remove(@NotNull UUID connectionUniqueId, @NotNull Key<T> key);

	/**
	 * Clears all stored values for the provided connection id.
	 *
	 * @param connectionUniqueId connection id
	 */
	void clear(@NotNull UUID connectionUniqueId);
}
