package me.whereareiam.identica.routing;

import me.whereareiam.identica.model.routing.attempt.RoutingAttemptDecision;
import me.whereareiam.identica.model.routing.attempt.RoutingAttemptReport;
import me.whereareiam.identica.model.routing.attempt.RoutingAttemptRequest;
import me.whereareiam.identica.model.routing.RoutingIntent;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;

/**
 * Coordinates platform-side attempts to apply a pending routing intent.
 * <p>
 * Platform adapters use this service before changing an initial server, overriding
 * a pre-connect target, or starting an explicit server connection. The service
 * keeps routing policy decisions in the common routing domain instead of the
 * platform implementation.
 */
public interface RoutingAttemptService {
	/**
	 * Decides whether the current routing intent may be attempted for the supplied
	 * platform trigger.
	 *
	 * @param request attempt request with connection id, trigger, and current server
	 * @return attempt decision containing the allowed intent or the skip/exhaustion reason
	 */
	@NotNull RoutingAttemptDecision decide(@NotNull RoutingAttemptRequest request);

	/**
	 * Records the result of a platform routing attempt.
	 *
	 * @param report attempt result reported by the platform adapter
	 */
	void record(@NotNull RoutingAttemptReport report);

	/**
	 * Returns the current routing intent for a connection without mutating it.
	 *
	 * @param connectionUniqueId connection unique id
	 * @return current intent, if one is pending or retained for the connection
	 */
	@NotNull Optional<RoutingIntent> current(@NotNull UUID connectionUniqueId);
}
