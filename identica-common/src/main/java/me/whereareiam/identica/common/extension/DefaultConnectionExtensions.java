package me.whereareiam.identica.common.extension;

import me.whereareiam.identica.Key;
import me.whereareiam.identica.connection.ConnectionExtensions;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultConnectionExtensions implements ConnectionExtensions {
	private final Map<UUID, Map<String, Object>> values = new ConcurrentHashMap<>();

	@Override
	public <T> void put(@NotNull UUID connectionUniqueId, @NotNull Key<T> key, @NotNull T value) {
		Map<String, Object> entry = values.computeIfAbsent(connectionUniqueId, ignored -> new ConcurrentHashMap<>());
		entry.put(key.getName(), value);
	}

	@Override
	public @NotNull <T> Optional<T> get(@NotNull UUID connectionUniqueId, @NotNull Key<T> key) {
		Map<String, Object> entry = values.get(connectionUniqueId);
		if (entry == null) return Optional.empty();
		return cast(key, entry.get(key.getName()));
	}

	@Override
	public @NotNull <T> Optional<T> remove(@NotNull UUID connectionUniqueId, @NotNull Key<T> key) {
		Map<String, Object> entry = values.get(connectionUniqueId);
		if (entry == null) return Optional.empty();

		Object removed = entry.remove(key.getName());
		if (entry.isEmpty()) values.remove(connectionUniqueId, entry);
		return cast(key, removed);
	}

	@Override
	public void clear(@NotNull UUID connectionUniqueId) {
		values.remove(connectionUniqueId);
	}

	private <T> Optional<T> cast(@NotNull Key<T> key, Object value) {
		if (key.getType().isInstance(value))
			return Optional.of(key.cast(value));

		return Optional.empty();
	}
}
