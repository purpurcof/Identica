package me.whereareiam.identica.common.extension;

import me.whereareiam.identica.Key;
import me.whereareiam.identica.registry.PreLoginExtensions;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultPreLoginExtensions implements PreLoginExtensions {
	private final Map<String, Map<String, Entry>> values = new ConcurrentHashMap<>();

	@Override
	public <T> void put(@NotNull String username, @NotNull Key<T> key, @NotNull T value, long ttlMs) {
		if (ttlMs <= 0) return;
		String userKey = normalize(username);
		if (userKey == null) return;

		Map<String, Entry> entry = values.computeIfAbsent(userKey, ignored -> new ConcurrentHashMap<>());
		entry.put(key.getName(), new Entry(value, System.currentTimeMillis() + ttlMs));
	}

	@Override
	public @NotNull <T> Optional<T> peek(@NotNull String username, @NotNull Key<T> key) {
		return read(username, key, false);
	}

	@Override
	public @NotNull <T> Optional<T> consume(@NotNull String username, @NotNull Key<T> key) {
		return read(username, key, true);
	}

	@Override
	public void clear(@NotNull String username) {
		String userKey = normalize(username);
		if (userKey == null) return;
		values.remove(userKey);
	}

	private <T> Optional<T> read(@NotNull String username, @NotNull Key<T> key, boolean consume) {
		String userKey = normalize(username);
		if (userKey == null) return Optional.empty();

		Map<String, Entry> entry = values.get(userKey);
		if (entry == null) return Optional.empty();

		Entry stored = entry.get(key.getName());
		if (stored == null) return Optional.empty();

		if (stored.expiresAt > 0 && stored.expiresAt <= System.currentTimeMillis()) {
			entry.remove(key.getName());
			if (entry.isEmpty()) values.remove(userKey, entry);
			return Optional.empty();
		}

		if (consume) {
			entry.remove(key.getName());
			if (entry.isEmpty()) values.remove(userKey, entry);
		}

		return cast(key, stored.value);
	}

	private String normalize(String username) {
		if (username == null || username.isBlank())
			return null;

		return username.trim().toLowerCase(Locale.ROOT);
	}

	private <T> Optional<T> cast(@NotNull Key<T> key, Object value) {
		if (key.getType().isInstance(value))
			return Optional.of(key.cast(value));

		return Optional.empty();
	}

	private record Entry(Object value, long expiresAt) {
	}
}
