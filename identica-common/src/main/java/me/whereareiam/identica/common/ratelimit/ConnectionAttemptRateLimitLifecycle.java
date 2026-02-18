package me.whereareiam.identica.common.ratelimit;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.whereareiam.identica.event.EventListener;
import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.event.connection.attempt.ConnectionAdvanceAttemptEvent;
import me.whereareiam.identica.event.connection.attempt.ConnectionAttemptEvent;
import me.whereareiam.identica.event.connection.attempt.ConnectionProcessAttemptEvent;
import me.whereareiam.identica.event.connection.attempt.ConnectionResumeAttemptEvent;
import me.whereareiam.identica.event.base.IdenticEvent;
import me.whereareiam.identica.model.auth.ConnectionDecision;
import me.whereareiam.identica.model.ratelimit.RateLimitContext;
import me.whereareiam.identica.model.ratelimit.RateLimitDecision;
import me.whereareiam.identica.ratelimit.RateLimitService;
import me.whereareiam.identica.type.ratelimit.RateLimitScope;
import org.jetbrains.annotations.NotNull;

@Singleton
public class ConnectionAttemptRateLimitLifecycle implements EventListener {
	private final RateLimitService rateLimitService;

	@Inject
	public ConnectionAttemptRateLimitLifecycle(RateLimitService rateLimitService, EventManager eventManager) {
		this.rateLimitService = rateLimitService;
		eventManager.register(this);
	}

	@IdenticEvent
	public void onConnectionAttempt(@NotNull ConnectionAttemptEvent event) {
		if (event.getDecision() != null) return;

		RateLimitScope scope = toScope(event);
		if (scope == null) return;

		RateLimitContext context = RateLimitContext.builder()
				.connectionUniqueId(event.getConnectionUniqueId())
				.uniqueId(event.getIdentityUniqueId())
				.username(event.getUsername())
				.ip(event.getIp())
				.build();

		RateLimitDecision decision = rateLimitService.evaluate(scope, context).orElse(null);
		if (decision == null || !decision.isLimited() || !decision.isDeny()) return;

		event.setDecision(ConnectionDecision.deny(decision.getMessage()));
	}

	private RateLimitScope toScope(@NotNull ConnectionAttemptEvent event) {
		return switch (event) {
			case ConnectionProcessAttemptEvent _ -> RateLimitScope.PROCESS;
			case ConnectionResumeAttemptEvent _ -> RateLimitScope.RESUME;
			case ConnectionAdvanceAttemptEvent _ -> RateLimitScope.ADVANCE;
			default -> null;
		};
	}
}
