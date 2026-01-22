package me.whereareiam.identica.routing;

import me.whereareiam.identica.auth.step.AuthenticationStep;
import me.whereareiam.identica.model.auth.AuthContext;
import me.whereareiam.identica.model.RoutingTarget;

import java.util.Optional;

/**
 * Resolves routing targets for interactive steps and completed authentication.
 */
public interface RoutingService {
	Optional<RoutingTarget> resolveStepTarget(
			AuthContext context,
			String providerId,
			AuthenticationStep step
	);

	Optional<RoutingTarget> resolveCompletionTarget(AuthContext context);
}
