package me.whereareiam.identica.actor;

import me.whereareiam.identica.Key;
import me.whereareiam.keystone.Actor;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Abstract base class for Identica identity implementations.
 * Platform-specific modules extend this with concrete implementations.
 */
@SuppressWarnings("unused")
public abstract class Identity extends OfflineIdentity implements Actor {
	@NotNull
	protected final UUID uniqueId;
	private final @NotNull Map<String, Object> metadata = new ConcurrentHashMap<>();

	protected Identity(@NotNull UUID uniqueId, @NotNull String username) {
		this(uniqueId, username, null);
	}

	protected Identity(@NotNull UUID uniqueId, @NotNull String username, String ip) {
		super(username, ip);
		this.uniqueId = uniqueId;
	}

	/**
	 * Returns the Identica unique id for this identity.
	 *
	 * @return unique identity id
	 */
	@Override
	public @NotNull UUID getUniqueId() {
		return uniqueId;
	}

	/**
	 * Sends a message to this identity.
	 *
	 * @param message message component to send
	 */
	@Override
	public abstract void sendMessage(@NotNull Component message);

	/**
	 * Checks whether this identity has the specified permission.
	 *
	 * @param permission permission node
	 * @return {@code true} when the permission is granted
	 */
	@Override
	public abstract boolean hasPermission(@NotNull String permission);

	/**
	 * Returns the locale for this identity.
	 *
	 * @return locale associated with this identity
	 */
	@Override
	public abstract @NotNull Locale getLocale();

	/**
	 * Returns the Adventure audience for this identity.
	 *
	 * @return audience instance
	 */
	@Override
	public abstract @NotNull Audience getAudience();

	/**
	 * Disconnects this identity from the server.
	 *
	 * @param reason disconnect message
	 */
	public abstract void disconnect(@NotNull Component reason);

	/**
	 * Returns the metadata map for this identity.
	 *
	 * @return mutable metadata map
	 */
	@NotNull
	public Map<String, Object> metadata() {
		return metadata;
	}

	/**
	 * Stores a metadata value for the given key.
	 *
	 * @param key metadata key
	 * @param value metadata value
	 * @param <T> value type
	 */
	public <T> void set(@NotNull Key<T> key, @NotNull T value) {
		metadata.put(key.getName(), value);
	}

	/**
	 * Reads a metadata value for the given key.
	 *
	 * @param key metadata key
	 * @param <T> value type
	 * @return optional containing the value when present and type-safe
	 */
	@NotNull
	public <T> Optional<T> get(@NotNull Key<T> key) {
		Object value = metadata.get(key.getName());
		if (key.getType().isInstance(value))
			return Optional.of(key.cast(value));

		return Optional.empty();
	}

	/**
	 * Reads a metadata value or returns the provided default.
	 *
	 * @param key metadata key
	 * @param defaultValue value returned when missing
	 * @param <T> value type
	 * @return stored value or default
	 */
	@NotNull
	public <T> T getOrDefault(@NotNull Key<T> key, @NotNull T defaultValue) {
		return get(key).orElse(defaultValue);
	}

	/**
	 * Removes a metadata entry for the given key.
	 *
	 * @param key metadata key
	 * @param <T> value type
	 * @return optional containing the removed value when present
	 */
	@NotNull
	public <T> Optional<T> remove(@NotNull Key<T> key) {
		Object value = metadata.remove(key.getName());
		if (key.getType().isInstance(value))
			return Optional.of(key.cast(value));

		return Optional.empty();
	}

	/**
	 * Checks if metadata exists for the provided key.
	 *
	 * @param key metadata key
	 * @return {@code true} when a value is present and type-safe
	 */
	public boolean has(@NotNull Key<?> key) {
		Object value = metadata.get(key.getName());
		return key.getType().isInstance(value);
	}

	/**
	 * Clears all metadata entries.
	 */
	public void clearMetadata() {
		metadata.clear();
	}

	/**
	 * Copies metadata from another identity.
	 *
	 * @param other identity to copy from
	 */
	public void syncMetadataFrom(@NotNull Identity other) {
		if (other == this) return;
		metadata.clear();
		metadata.putAll(other.metadata());
	}
}
