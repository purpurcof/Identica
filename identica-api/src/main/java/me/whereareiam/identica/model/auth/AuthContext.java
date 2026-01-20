package me.whereareiam.identica.model.auth;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.util.Key;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Authentication context containing connection info and mutable state.
 */
@Getter
@RequiredArgsConstructor
public class AuthContext {
	private final String username;
	private final String ip;
	private final UUID connectionUniqueId;
	private final String intendedServer;

	private final Map<Key<?>, Object> data = new HashMap<>();

	public <T> void put(Key<T> key, T value) {
		data.put(key, value);
	}

	public <T> Optional<T> get(Key<T> key) {
		Object value = data.get(key);
		if (value == null) {
			return Optional.empty();
		}
		return Optional.of(key.cast(value));
	}

	public <T> T getOrDefault(Key<T> key, T defaultValue) {
		return get(key).orElse(defaultValue);
	}

	public boolean has(Key<?> key) {
		return data.containsKey(key);
	}

	public <T> Optional<T> remove(Key<T> key) {
		Object value = data.remove(key);
		if (value == null) {
			return Optional.empty();
		}
		return Optional.of(key.cast(value));
	}

}
