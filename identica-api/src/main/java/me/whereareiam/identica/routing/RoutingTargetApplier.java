package me.whereareiam.identica.routing;

import me.whereareiam.identica.model.RoutingTarget;
import me.whereareiam.identica.pipeline.ScenarioContext;

/**
 * Platform adapter that applies routing targets to a live connection.
 */
public interface RoutingTargetApplier {
	void apply(RoutingTarget target, ScenarioContext context);
}
