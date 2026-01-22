package me.whereareiam.identica.routing;

import me.whereareiam.identica.model.RoutingTarget;
import me.whereareiam.identica.model.auth.AuthContext;

/**
 * Platform adapter that applies routing targets to a live connection.
 */
public interface RoutingTargetApplier {
	void apply(RoutingTarget target, AuthContext context);
}
