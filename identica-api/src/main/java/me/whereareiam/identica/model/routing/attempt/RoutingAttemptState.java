package me.whereareiam.identica.model.routing.attempt;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import me.whereareiam.identica.type.routing.reason.RoutingAttemptFailureReason;

/**
 * Mutable attempt counters for a routing intent.
 */
@Getter
@Setter
@ToString
public class RoutingAttemptState {
	private int attempts;
	private long lastAttemptAt;
	private String lastServer;
	private boolean lastAccepted;
	private RoutingAttemptFailureReason lastFailureReason;

	public RoutingAttemptState copy() {
		RoutingAttemptState copy = new RoutingAttemptState();
		copy.setAttempts(attempts);
		copy.setLastAttemptAt(lastAttemptAt);
		copy.setLastServer(lastServer);
		copy.setLastAccepted(lastAccepted);
		copy.setLastFailureReason(lastFailureReason);
		return copy;
	}
}
