package me.whereareiam.identica.common.routing;

import com.google.inject.Singleton;
import me.whereareiam.identica.model.RoutingTarget;
import me.whereareiam.identica.routing.RoutingStateStore;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class DefaultRoutingStateStore implements RoutingStateStore {
	private final Map<UUID, RoutingTarget> targets = new ConcurrentHashMap<>();

	@Override
	public void put(@NotNull UUID connectionUniqueId, @NotNull RoutingTarget target) {
		targets.put(connectionUniqueId, target);
	}

	@Override
	public @NotNull Optional<RoutingTarget> peek(@NotNull UUID connectionUniqueId) {
		return Optional.ofNullable(targets.get(connectionUniqueId));
	}

	@Override
	public @NotNull Optional<RoutingTarget> consume(@NotNull UUID connectionUniqueId) {
		RoutingTarget current = targets.remove(connectionUniqueId);
		return Optional.ofNullable(current);
	}

	@Override
	public boolean clear(@NotNull UUID connectionUniqueId) {
		return targets.remove(connectionUniqueId) != null;
	}
}
