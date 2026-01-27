package me.whereareiam.identica.model.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import me.whereareiam.identica.Key;
import me.whereareiam.identica.actor.OfflineIdentity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Authentication context containing connection info and mutable state.
 */
@Getter
@AllArgsConstructor
@Builder(toBuilder = true)
@SuppressWarnings("unused")
public class AuthContext {
	private final @Nullable UUID connectionUniqueId;
	private final @NotNull ConnectionInfo connectionInfo;
	private final @Nullable String intendedServer;

	@Setter
	private @Nullable AuthContext.Provider provider;

	@Builder.Default
	private final @NotNull Map<Key<?>, Object> data = new HashMap<>();

	/**
	 * Returns the offline identity attached to this connection, if any.
	 *
	 * @return offline identity or {@code null}
	 */
	public @Nullable OfflineIdentity getIdentity() {
		return connectionInfo.getIdentity();
	}

	/**
	 * Returns whether the connection is in online mode.
	 *
	 * @return {@code true} when online mode is enabled
	 */
	public boolean isOnlineMode() {
		return connectionInfo.isOnlineMode();
	}

	/**
	 * Returns the Identica unique id assigned to this connection.
	 *
	 * @return unique id or {@code null}
	 */
	public @Nullable UUID getIdenticaUniqueId() {
		return connectionInfo.getIdenticaUniqueId();
	}

	/**
	 * Sets the Identica unique id for this connection.
	 *
	 * @param identicaUniqueId unique id to assign
	 */
	public void setIdenticaUniqueId(@Nullable UUID identicaUniqueId) {
		connectionInfo.setIdenticaUniqueId(identicaUniqueId);
	}

	/**
	 * Returns the username for this connection.
	 *
	 * @return username or {@code null}
	 */
	public @Nullable String getUsername() {
		return connectionInfo.getUsername();
	}

	/**
	 * Returns the IP address for this connection.
	 *
	 * @return IP address or {@code null}
	 */
	public @Nullable String getIp() {
		return connectionInfo.getIp();
	}

	/**
	 * Stores a custom value in the context data map.
	 *
	 * @param key data key
	 * @param value value to store
	 * @param <T> value type
	 */
	public <T> void put(@NotNull Key<T> key, @Nullable T value) {
		data.put(key, value);
	}

	/**
	 * Retrieves a typed value from the context data map.
	 *
	 * @param key data key
	 * @param <T> value type
	 * @return optional containing the value when present
	 */
	public <T> @NotNull Optional<T> get(@NotNull Key<T> key) {
		Object value = data.get(key);
		if (value == null)
			return Optional.empty();
		return Optional.of(key.cast(value));
	}

	/**
	 * Retrieves a typed value or returns the provided default.
	 *
	 * @param key data key
	 * @param defaultValue fallback value
	 * @param <T> value type
	 * @return stored value or default
	 */
	public <T> @Nullable T getOrDefault(@NotNull Key<T> key, @Nullable T defaultValue) {
		return get(key).orElse(defaultValue);
	}

	/**
	 * Checks if the context contains the given key.
	 *
	 * @param key data key
	 * @return {@code true} when a value is present
	 */
	public boolean has(@NotNull Key<?> key) {
		return data.containsKey(key);
	}

	/**
	 * Removes a value from the context data map.
	 *
	 * @param key data key
	 * @param <T> value type
	 * @return optional containing the removed value when present
	 */
	public <T> @NotNull Optional<T> remove(@NotNull Key<T> key) {
		Object value = data.remove(key);
		if (value == null)
			return Optional.empty();
		return Optional.of(key.cast(value));
	}

	/**
	 * Provider claim captured after authentication.
	 */
	@Getter
	@Setter
	@ToString
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder(toBuilder = true)
	public static class Provider {
		private @Nullable String providerId;
		private @Nullable String providerSubject;
		private @NotNull String providerUsername;
	}

}
