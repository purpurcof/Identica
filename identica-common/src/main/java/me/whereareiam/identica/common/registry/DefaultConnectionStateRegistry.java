package me.whereareiam.identica.common.registry;

import com.google.inject.Singleton;
import me.whereareiam.identica.model.connection.ConnectionState;
import me.whereareiam.identica.registry.ConnectionStateRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class DefaultConnectionStateRegistry implements ConnectionStateRegistry {
	private final Map<UUID, ConnectionState> states = new ConcurrentHashMap<>();

	@Override
	public @NotNull ConnectionState ensure(@NotNull UUID connectionUniqueId) {
		return states.computeIfAbsent(connectionUniqueId, ConnectionState::new);
	}

	@Override
	public @NotNull Optional<ConnectionState> find(@NotNull UUID connectionUniqueId) {
		return Optional.ofNullable(states.get(connectionUniqueId));
	}

	@Override
	public @NotNull Collection<ConnectionState> getStates() {
		return states.values();
	}

	@Override
	public boolean clear(@NotNull UUID connectionUniqueId) {
		return states.remove(connectionUniqueId) != null;
	}
}
