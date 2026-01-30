package me.whereareiam.identica.routing;

import me.whereareiam.identica.model.RoutingTarget;

import java.util.Optional;

/**
 * Resolves routing targets from routing decisions.
 */
public interface RoutingService {
	/**
	 * Resolves a routing target for the given decision.
	 *
	 * @param decision routing decision
	 * @return resolved routing target if applicable
	 */
	Optional<RoutingTarget> resolve(RoutingDecision decision);
}
