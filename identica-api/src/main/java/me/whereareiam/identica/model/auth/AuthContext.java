package me.whereareiam.identica.model.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import me.whereareiam.identica.Key;
import me.whereareiam.identica.identity.actor.ConnectionIdentity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Authentication context containing connection identity and mutable state.
 */
@Getter
@AllArgsConstructor
@Builder(toBuilder = true)
@SuppressWarnings("unused")
public class AuthContext {
	private final @Nullable UUID connectionUniqueId;
	private final @NotNull ConnectionIdentity identity;
	private final @Nullable String intendedServer;

	@Setter
	private @Nullable AuthContext.Provider provider;

	@Builder.Default
	private final @NotNull Map<Key<?>, Object> data = new HashMap<>();

	/**
	 * Returns the connection identity attached to this context.
	 *
	 * @return connection identity
	 */
	public @NotNull ConnectionIdentity getIdentity() {
		return identity;
	}

	/**
	 * Returns the Identica unique id assigned to this connection.
	 *
	 * @return unique id or {@code null}
	 */
	public @Nullable UUID getIdenticaUniqueId() {
		return identity.getUniqueId();
	}

	/**
	 * Sets the Identica unique id for this connection.
	 *
	 * @param identicaUniqueId unique id to assign
	 */
	public void setIdenticaUniqueId(@Nullable UUID identicaUniqueId) {
		identity.setUniqueId(identicaUniqueId);
	}

	/**
	 * Returns the username for this connection.
	 *
	 * @return username or {@code null}
	 */
	public @Nullable String getUsername() {
		return identity.getUsername();
	}

	/**
	 * Returns the IP address for this connection.
	 *
	 * @return IP address or {@code null}
	 */
	public @Nullable String getIp() {
		return identity.getIp();
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
