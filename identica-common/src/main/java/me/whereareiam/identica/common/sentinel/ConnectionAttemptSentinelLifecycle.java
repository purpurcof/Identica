package me.whereareiam.identica.common.sentinel;

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
import me.whereareiam.identica.model.sentinel.SentinelContext;
import me.whereareiam.identica.model.sentinel.SentinelDecision;
import me.whereareiam.identica.sentinel.SentinelService;
import me.whereareiam.identica.type.sentinel.SentinelScope;
import org.jetbrains.annotations.NotNull;

@Singleton
public class ConnectionAttemptSentinelLifecycle implements EventListener {
	private final SentinelService sentinelService;

	@Inject
	public ConnectionAttemptSentinelLifecycle(SentinelService sentinelService, EventManager eventManager) {
		this.sentinelService = sentinelService;
		eventManager.register(this);
	}

	@IdenticEvent
	public void onConnectionAttempt(@NotNull ConnectionAttemptEvent event) {
		if (event.getDecision() != null) return;

		SentinelScope scope = toScope(event);
		if (scope == null) return;

		SentinelContext context = SentinelContext.builder()
				.connectionUniqueId(event.getConnectionUniqueId())
				.uniqueId(event.getIdentityUniqueId())
				.username(event.getUsername())
				.ip(event.getIp())
				.build();

		SentinelDecision decision = sentinelService.evaluate(scope, context).orElse(null);
		if (decision == null || !decision.isLimited() || !decision.isDeny()) return;

		event.setDecision(ConnectionDecision.deny(decision.getMessage()));
	}

	private SentinelScope toScope(@NotNull ConnectionAttemptEvent event) {
		return switch (event) {
			case ConnectionProcessAttemptEvent _ -> SentinelScope.PROCESS;
			case ConnectionResumeAttemptEvent _ -> SentinelScope.RESUME;
			case ConnectionAdvanceAttemptEvent _ -> SentinelScope.ADVANCE;
			default -> null;
		};
	}
}
