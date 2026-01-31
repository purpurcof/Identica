package me.whereareiam.identica.identity.registry;

import me.whereareiam.identica.Key;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;

/**
 * Registry for identity-scoped extension data keyed by Identica unique id.
 */
public interface IdentityExtensions {
	/**
	 * Stores a value for the provided identity id and key.
	 *
	 * @param uniqueId identity id
	 * @param key key describing the stored value
	 * @param value value to store
	 * @param <T> value type
	 */
	<T> void put(@NotNull UUID uniqueId, @NotNull Key<T> key, @NotNull T value);

	/**
	 * Returns a stored value for the provided identity id and key.
	 *
	 * @param uniqueId identity id
	 * @param key key describing the stored value
	 * @param <T> value type
	 * @return optional containing the value when present
	 */
	@NotNull <T> Optional<T> get(@NotNull UUID uniqueId, @NotNull Key<T> key);

	/**
	 * Removes a stored value for the provided identity id and key.
	 *
	 * @param uniqueId identity id
	 * @param key key describing the stored value
	 * @param <T> value type
	 * @return optional containing the removed value when present
	 */
	@NotNull <T> Optional<T> remove(@NotNull UUID uniqueId, @NotNull Key<T> key);

	/**
	 * Clears all stored values for the provided identity id.
	 *
	 * @param uniqueId identity id
	 */
	void clear(@NotNull UUID uniqueId);
}
