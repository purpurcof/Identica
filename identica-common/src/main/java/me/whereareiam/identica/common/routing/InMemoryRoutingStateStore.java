package me.whereareiam.identica.common.routing;

import com.google.inject.Singleton;
import me.whereareiam.identica.model.RoutingTarget;
import me.whereareiam.identica.routing.RoutingStateStore;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class InMemoryRoutingStateStore implements RoutingStateStore {
	private final ConcurrentHashMap<UUID, RoutingTarget> targets = new ConcurrentHashMap<>();

	@Override
	public void put(UUID connectionUniqueId, RoutingTarget target) {
		if (connectionUniqueId == null || target == null) return;
		targets.put(connectionUniqueId, target);
	}

	@Override
	public Optional<RoutingTarget> peek(UUID connectionUniqueId) {
		if (connectionUniqueId == null) return Optional.empty();
		return Optional.ofNullable(targets.get(connectionUniqueId));
	}

	@Override
	public Optional<RoutingTarget> consume(UUID connectionUniqueId) {
		if (connectionUniqueId == null) return Optional.empty();
		return Optional.ofNullable(targets.remove(connectionUniqueId));
	}

	@Override
	public boolean clear(UUID connectionUniqueId) {
		if (connectionUniqueId == null) return false;
		return targets.remove(connectionUniqueId) != null;
	}
}
