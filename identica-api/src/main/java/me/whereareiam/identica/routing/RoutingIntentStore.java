package me.whereareiam.identica.routing;

import me.whereareiam.identica.model.routing.attempt.RoutingAttemptReport;
import me.whereareiam.identica.model.routing.RoutingIntent;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;

/**
 * Store for connection-scoped routing intents.
 * <p>
 * Implementations keep at most one active routing intent per connection id and
 * track mutable attempt/reachability state for that intent.
 */
public interface RoutingIntentStore {
	/**
	 * Stores or replaces the routing intent for its connection.
	 *
	 * @param intent routing intent to store
	 */
	void put(@NotNull RoutingIntent intent);

	/**
	 * Returns the routing intent without removing it.
	 *
	 * @param connectionUniqueId connection unique id
	 * @return current routing intent, if one exists
	 */
	@NotNull Optional<RoutingIntent> peek(@NotNull UUID connectionUniqueId);

	/**
	 * Returns and removes the routing intent for a connection.
	 *
	 * @param connectionUniqueId connection unique id
	 * @return removed routing intent, if one existed
	 */
	@NotNull Optional<RoutingIntent> consume(@NotNull UUID connectionUniqueId);

	/**
	 * Marks the current routing intent as reached when the server matches its endpoint.
	 *
	 * @param connectionUniqueId connection unique id
	 * @param serverName reached server name
	 * @return updated routing intent when the server matches the current endpoint
	 */
	@NotNull Optional<RoutingIntent> markReached(@NotNull UUID connectionUniqueId, @NotNull String serverName);

	/**
	 * Marks the current routing intent as exhausted.
	 *
	 * @param connectionUniqueId connection unique id
	 * @return exhausted routing intent, if one exists
	 */
	@NotNull Optional<RoutingIntent> markExhausted(@NotNull UUID connectionUniqueId);

	/**
	 * Records an attempt result against the current routing intent.
	 *
	 * @param report platform attempt report
	 * @return updated routing intent, if one exists for the report connection
	 */
	@NotNull Optional<RoutingIntent> recordAttempt(@NotNull RoutingAttemptReport report);

	/**
	 * Removes the routing intent for a connection.
	 *
	 * @param connectionUniqueId connection unique id
	 * @return {@code true} if an intent was removed
	 */
	boolean clear(@NotNull UUID connectionUniqueId);
}
