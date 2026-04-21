package me.whereareiam.identica.common.routing;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.event.routing.attempt.RoutingAttemptFinishedEvent;
import me.whereareiam.identica.event.routing.attempt.RoutingAttemptStartedEvent;
import me.whereareiam.identica.event.routing.completion.CompletionRoutingReachedEvent;
import me.whereareiam.identica.event.routing.completion.CompletionRoutingStartedEvent;
import me.whereareiam.identica.event.routing.intent.RoutingIntentClearedEvent;
import me.whereareiam.identica.event.routing.intent.RoutingIntentExhaustedEvent;
import me.whereareiam.identica.event.routing.intent.RoutingIntentReachedEvent;
import me.whereareiam.identica.event.routing.intent.RoutingIntentStartedEvent;
import me.whereareiam.identica.event.routing.intent.RoutingIntentUpdatedEvent;
import me.whereareiam.identica.event.routing.step.StepRoutingReachedEvent;
import me.whereareiam.identica.event.routing.step.StepRoutingStartedEvent;
import me.whereareiam.identica.model.routing.attempt.RoutingAttemptDecision;
import me.whereareiam.identica.model.routing.attempt.RoutingAttemptReport;
import me.whereareiam.identica.model.routing.attempt.RoutingAttemptRequest;
import me.whereareiam.identica.model.routing.RoutingIntent;
import me.whereareiam.identica.model.routing.RoutingPlan;
import me.whereareiam.identica.model.routing.RoutingSignal;
import me.whereareiam.identica.routing.RoutingAttemptService;
import me.whereareiam.identica.routing.RoutingCoordinator;
import me.whereareiam.identica.routing.RoutingIntentStore;
import me.whereareiam.identica.type.routing.RoutingClearReason;
import me.whereareiam.identica.type.routing.RoutingIntentStatus;
import me.whereareiam.identica.type.routing.RoutingPlanAction;
import me.whereareiam.identica.type.routing.RoutingReason;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class DefaultRoutingCoordinator implements RoutingCoordinator, RoutingAttemptService {
	private final RoutingPlanner routingPlanner;
	private final RoutingIntentStore routingIntentStore;
	private final EventManager eventManager;

	@Override
	public void accept(@NotNull RoutingSignal signal) {
		apply(routingPlanner.plan(signal));
	}

	@Override
	public void markReached(@NotNull UUID connectionUniqueId, @NotNull String serverName) {
		RoutingIntent intent = routingIntentStore.markReached(connectionUniqueId, serverName).orElse(null);
		if (intent == null) return;

		publishReached(intent, serverName);
		if (intent.getAttemptPolicy().isConsumeOnReached())
			clear(connectionUniqueId, RoutingClearReason.REACHED);
	}

	@Override
	public void clear(@NotNull UUID connectionUniqueId, @NotNull RoutingClearReason reason) {
		RoutingIntent intent = routingIntentStore.consume(connectionUniqueId).orElse(null);
		if (intent != null) {
			intent.setStatus(RoutingIntentStatus.CLEARED);
			intent.setUpdatedAt(System.currentTimeMillis());
		}

		eventManager.call(new RoutingIntentClearedEvent(connectionUniqueId, intent, reason));
	}

	@Override
	public @NotNull RoutingAttemptDecision decide(@NotNull RoutingAttemptRequest request) {
		RoutingIntent intent = routingIntentStore.peek(request.getConnectionUniqueId()).orElse(null);
		if (intent == null) return RoutingAttemptDecision.skipped("no-intent");
		if (intent.getStatus() != RoutingIntentStatus.PENDING)
			return RoutingAttemptDecision.skipped("intent-not-pending");

		String currentServer = request.getCurrentServer();
		if (currentServer != null && currentServer.equalsIgnoreCase(intent.getEndpoint().getServer()))
			return RoutingAttemptDecision.skipped("already-reached");

		if (!intent.getAttemptPolicy().allowsAttempt(intent.getAttemptState().getAttempts())) {
			exhaust(intent);
			return RoutingAttemptDecision.exhausted(intent);
		}

		eventManager.call(new RoutingAttemptStartedEvent(intent, request));
		return RoutingAttemptDecision.allowed(intent);
	}

	@Override
	public void record(@NotNull RoutingAttemptReport report) {
		RoutingIntent intent = routingIntentStore.recordAttempt(report).orElse(null);
		if (intent == null) return;

		eventManager.call(new RoutingAttemptFinishedEvent(intent, report));
	}

	@Override
	public @NotNull Optional<RoutingIntent> current(@NotNull UUID connectionUniqueId) {
		return routingIntentStore.peek(connectionUniqueId);
	}

	private void apply(@NotNull RoutingPlan plan) {
		RoutingPlanAction action = plan.getAction();
		if (action == RoutingPlanAction.IGNORE) return;

		if (action == RoutingPlanAction.CLEAR) {
			if (plan.getConnectionUniqueId() != null && plan.getClearReason() != null)
				clear(plan.getConnectionUniqueId(), plan.getClearReason());
			return;
		}

		RoutingIntent intent = plan.getIntent();
		if (intent == null) return;

		RoutingIntent previous = routingIntentStore.consume(intent.getConnectionUniqueId()).orElse(null);
		if (previous != null)
			eventManager.call(new RoutingIntentClearedEvent(intent.getConnectionUniqueId(), previous, RoutingClearReason.REPLACED));

		routingIntentStore.put(intent);
		if (action == RoutingPlanAction.START || previous == null) {
			publishStarted(intent);
			return;
		}

		eventManager.call(new RoutingIntentUpdatedEvent(intent));
	}

	private void exhaust(@NotNull RoutingIntent intent) {
		routingIntentStore.markExhausted(intent.getConnectionUniqueId());
		eventManager.call(new RoutingIntentExhaustedEvent(intent));
		if (intent.getAttemptPolicy().isConsumeOnExhausted())
			clear(intent.getConnectionUniqueId(), RoutingClearReason.EXHAUSTED);
	}

	private void publishStarted(@NotNull RoutingIntent intent) {
		eventManager.call(new RoutingIntentStartedEvent(intent));
		if (intent.getReason() == RoutingReason.STEP)
			eventManager.call(new StepRoutingStartedEvent(intent));
		if (intent.getReason() == RoutingReason.COMPLETION)
			eventManager.call(new CompletionRoutingStartedEvent(intent));
	}

	private void publishReached(@NotNull RoutingIntent intent, @NotNull String currentServer) {
		eventManager.call(new RoutingIntentReachedEvent(intent, currentServer));
		if (intent.getReason() == RoutingReason.STEP)
			eventManager.call(new StepRoutingReachedEvent(intent, currentServer));
		if (intent.getReason() == RoutingReason.COMPLETION)
			eventManager.call(new CompletionRoutingReachedEvent(intent, currentServer));
	}
}
