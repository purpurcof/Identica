package me.whereareiam.identica.routing;

import me.whereareiam.identica.model.RoutingTarget;

import java.util.Optional;
import java.util.UUID;

/**
 * Stores routing targets per connection.
 */
public interface RoutingStateStore {
	void put(UUID connectionUniqueId, RoutingTarget target);

	Optional<RoutingTarget> peek(UUID connectionUniqueId);

	Optional<RoutingTarget> consume(UUID connectionUniqueId);

	boolean clear(UUID connectionUniqueId);
}
