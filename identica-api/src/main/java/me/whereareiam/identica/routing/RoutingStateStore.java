package me.whereareiam.identica.routing;

import me.whereareiam.identica.model.RoutingTarget;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;

/**
 * Store for connection-scoped routing targets.
 */
@SuppressWarnings("unused")
public interface RoutingStateStore {
	/**
	 * Stores a routing target for a connection.
	 *
	 * @param connectionUniqueId connection unique id
	 * @param target routing target
	 */
	void put(@NotNull UUID connectionUniqueId, @NotNull RoutingTarget target);

	/**
	 * Returns the routing target without clearing it.
	 *
	 * @param connectionUniqueId connection unique id
	 * @return optional routing target
	 */
	@NotNull Optional<RoutingTarget> peek(@NotNull UUID connectionUniqueId);

	/**
	 * Returns the routing target and clears it.
	 *
	 * @param connectionUniqueId connection unique id
	 * @return optional routing target
	 */
	@NotNull Optional<RoutingTarget> consume(@NotNull UUID connectionUniqueId);

	/**
	 * Clears the routing target for a connection.
	 *
	 * @param connectionUniqueId connection unique id
	 * @return {@code true} if a target was cleared
	 */
	boolean clear(@NotNull UUID connectionUniqueId);
}
