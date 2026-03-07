package me.whereareiam.identica.common.routing;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.whereareiam.identica.event.EventListener;
import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.event.connection.ConnectionPendingClearedEvent;
import me.whereareiam.identica.event.base.IdenticEvent;
import me.whereareiam.identica.event.routing.RoutingTargetUpdatedEvent;
import me.whereareiam.identica.event.pipeline.attempt.PipelineAttemptFinishedEvent;
import me.whereareiam.identica.event.step.StepFinishedEvent;
import me.whereareiam.identica.model.RoutingTarget;
import me.whereareiam.identica.model.pipeline.journey.stage.step.StepResult;
import me.whereareiam.identica.pipeline.ScenarioContext;
import me.whereareiam.identica.routing.RoutingDecision;
import me.whereareiam.identica.routing.RoutingService;
import me.whereareiam.identica.routing.RoutingStateStore;
import me.whereareiam.identica.type.pipeline.PipelineStatus;
import me.whereareiam.identica.type.pipeline.PipelineType;
import me.whereareiam.identica.type.pipeline.journey.StageType;

import java.util.UUID;

@Singleton
public class RoutingLifecycle implements EventListener {
	private final RoutingService routingService;
	private final RoutingStateStore routingStateStore;
	private final EventManager eventManager;

	@Inject
	public RoutingLifecycle(
			RoutingService routingService,
			RoutingStateStore routingStateStore,
			EventManager eventManager
	) {
		this.routingService = routingService;
		this.routingStateStore = routingStateStore;
		this.eventManager = eventManager;
		eventManager.register(this);
	}

	@IdenticEvent
	public void onStepFinished(StepFinishedEvent event) {
		if (event == null) return;

		StepResult result = event.getResult();
		if (result.getStatus() == StepResult.StepStatus.WAITING) {
			RoutingDecision decision = new RoutingDecision(
					event.getContext(),
					event.getPipelineType(),
					event.getPhase(),
					event.getStep(),
					result
			);

			RoutingTarget target = routingService.resolve(decision).orElse(null);
			if (target == null) {
				clear(event.getContext());
				return;
			}

			store(event.getContext(), target);
			return;
		}
		if (result.getStatus() == StepResult.StepStatus.COMPLETE) return;

		clear(event.getContext());
	}

	@IdenticEvent
	public void onFlowAttemptFinished(PipelineAttemptFinishedEvent event) {
		if (event == null || event.getContext() == null || event.getResult() == null)
			return;

		PipelineStatus status = event.getResult().getStatus();
		if (status == PipelineStatus.WAITING) return;

		if (status != PipelineStatus.COMPLETE) {
			clear(event.getContext());
			return;
		}

		StepResult completion = StepResult.complete(event.getContext());
		PipelineType pipelineType = event.getPipelineType();
		RoutingDecision decision = new RoutingDecision(
				event.getContext(),
				pipelineType,
				StageType.END,
				null,
				completion
		);
		RoutingTarget target = routingService.resolve(decision).orElse(null);
		if (target == null) {
			clear(event.getContext());
			return;
		}

		store(event.getContext(), target);
	}

	@IdenticEvent
	public void onPendingCleared(ConnectionPendingClearedEvent event) {
		if (event == null || event.getConnectionUniqueId() == null) return;
		routingStateStore.clear(event.getConnectionUniqueId());
	}

	private void store(ScenarioContext context, RoutingTarget target) {
		UUID connectionId = context != null ? context.getConnectionUniqueId() : null;
		if (connectionId == null || target == null) return;

		routingStateStore.put(connectionId, target);
		eventManager.call(new RoutingTargetUpdatedEvent(connectionId, target, context));
	}

	private void clear(ScenarioContext context) {
		UUID connectionId = context != null ? context.getConnectionUniqueId() : null;
		if (connectionId == null) return;

		routingStateStore.clear(connectionId);
	}

}
