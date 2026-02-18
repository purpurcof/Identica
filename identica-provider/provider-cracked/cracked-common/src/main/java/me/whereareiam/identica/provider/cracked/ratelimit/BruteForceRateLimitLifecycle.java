package me.whereareiam.identica.provider.cracked.ratelimit;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.whereareiam.identica.event.EventListener;
import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.event.base.IdenticEvent;
import me.whereareiam.identica.model.ratelimit.RateLimitContext;
import me.whereareiam.identica.model.ratelimit.RateLimitDecision;
import me.whereareiam.identica.provider.cracked.CrackedConstants;
import me.whereareiam.identica.provider.cracked.event.authentication.AuthenticationAttemptDecision;
import me.whereareiam.identica.provider.cracked.event.authentication.AuthenticationAttemptFailedEvent;
import me.whereareiam.identica.provider.cracked.event.authentication.AuthenticationAttemptSucceededEvent;
import me.whereareiam.identica.provider.cracked.model.authentication.AuthenticationAttemptContext;
import me.whereareiam.identica.ratelimit.RateLimitService;
import org.jetbrains.annotations.NotNull;

@Singleton
public class BruteForceRateLimitLifecycle implements EventListener {
	private final RateLimitService rateLimitService;

	@Inject
	public BruteForceRateLimitLifecycle(RateLimitService rateLimitService, EventManager eventManager) {
		this.rateLimitService = rateLimitService;
		eventManager.register(this);
	}

	@IdenticEvent
	public void onCrackedAuthenticationFailed(@NotNull AuthenticationAttemptFailedEvent event) {
		if (event.getDecision() != null) return;

		RateLimitContext context = rateLimitContext(event);
		RateLimitDecision decision = rateLimitService.record(
				CrackedConstants.RATE_LIMIT.BRUTE_FORCE,
				context
		);
		event.setDecision(toDecision(decision));
	}

	@IdenticEvent
	public void onCrackedAuthenticationSucceeded(@NotNull AuthenticationAttemptSucceededEvent event) {
		RateLimitContext context = rateLimitContext(event);
		rateLimitService.clear(
				CrackedConstants.RATE_LIMIT.BRUTE_FORCE,
				context
		);
	}

	private RateLimitContext rateLimitContext(@NotNull AuthenticationAttemptFailedEvent event) {
		AuthenticationAttemptContext context = event.getContext();
		return RateLimitContext.builder()
				.connectionUniqueId(context.getConnectionUniqueId())
				.uniqueId(context.getIdentityUniqueId())
				.username(context.getUsername())
				.ip(context.getIp())
				.build();
	}

	private RateLimitContext rateLimitContext(@NotNull AuthenticationAttemptSucceededEvent event) {
		AuthenticationAttemptContext context = event.getContext();
		return RateLimitContext.builder()
				.connectionUniqueId(context.getConnectionUniqueId())
				.uniqueId(context.getIdentityUniqueId())
				.username(context.getUsername())
				.ip(context.getIp())
				.build();
	}

	private AuthenticationAttemptDecision toDecision(RateLimitDecision decision) {
		if (decision == null) return AuthenticationAttemptDecision.allow();

		boolean deny = decision.isLimited() && decision.isDeny();
		String denyMessage = deny ? decision.getMessage() : null;
		String warningMessage = decision.getWarningMessage();
		return new AuthenticationAttemptDecision(deny, denyMessage, warningMessage);
	}
}
