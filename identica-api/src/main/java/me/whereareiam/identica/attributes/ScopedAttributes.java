package me.whereareiam.identica.attributes;

import me.whereareiam.identica.Key;
import me.whereareiam.identica.type.AttributeScope;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Unified attribute store for scoped extension data.
 */
@SuppressWarnings("unused")
public interface ScopedAttributes {
	/**
	 * Stores a value for the provided scope and owner key without expiration.
	 *
	 * @param scope attribute scope
	 * @param ownerKey owner identifier within the scope
	 * @param key attribute key
	 * @param value value to store
	 * @param <T> value type
	 */
	<T> void put(
			@NotNull AttributeScope scope,
			@NotNull String ownerKey,
			@NotNull Key<T> key,
			@NotNull T value
	);

	/**
	 * Stores a value for the provided scope and owner key with expiration.
	 *
	 * @param scope attribute scope
	 * @param ownerKey owner identifier within the scope
	 * @param key attribute key
	 * @param value value to store
	 * @param ttlMs time-to-live in milliseconds
	 * @param <T> value type
	 */
	<T> void put(
			@NotNull AttributeScope scope,
			@NotNull String ownerKey,
			@NotNull Key<T> key,
			@NotNull T value,
			long ttlMs
	);

	/**
	 * Returns a stored value for the provided scope and owner key.
	 *
	 * @param scope attribute scope
	 * @param ownerKey owner identifier within the scope
	 * @param key attribute key
	 * @param <T> value type
	 * @return optional containing the value when present
	 */
	@NotNull <T> Optional<T> get(
			@NotNull AttributeScope scope,
			@NotNull String ownerKey,
			@NotNull Key<T> key
	);

	/**
	 * Removes a stored value for the provided scope and owner key.
	 *
	 * @param scope attribute scope
	 * @param ownerKey owner identifier within the scope
	 * @param key attribute key
	 * @param <T> value type
	 * @return optional containing the removed value when present
	 */
	@NotNull <T> Optional<T> remove(
			@NotNull AttributeScope scope,
			@NotNull String ownerKey,
			@NotNull Key<T> key
	);

	/**
	 * Clears all stored values for the provided scope and owner key.
	 *
	 * @param scope attribute scope
	 * @param ownerKey owner identifier within the scope
	 */
	void clear(@NotNull AttributeScope scope, @NotNull String ownerKey);
}
