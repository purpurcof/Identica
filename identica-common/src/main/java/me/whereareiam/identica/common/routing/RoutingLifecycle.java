package me.whereareiam.identica.common.routing;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.whereareiam.identica.IdenticaKeys;
import me.whereareiam.identica.event.EventListener;
import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.event.auth.AuthPendingClearedEvent;
import me.whereareiam.identica.event.base.IdenticEvent;
import me.whereareiam.identica.event.routing.RoutingTargetUpdatedEvent;
import me.whereareiam.identica.event.step.StepFinishedEvent;
import me.whereareiam.identica.event.step.StepPrepareEvent;
import me.whereareiam.identica.model.RoutingTarget;
import me.whereareiam.identica.model.auth.AuthContext;
import me.whereareiam.identica.model.auth.StepResult;
import me.whereareiam.identica.model.connection.ConnectionState;
import me.whereareiam.identica.registry.ConnectionStateRegistry;
import me.whereareiam.identica.routing.RoutingDecision;
import me.whereareiam.identica.routing.RoutingService;
import me.whereareiam.identica.routing.RoutingTargetApplier;
import me.whereareiam.identica.type.step.AuthFlowType;

import java.util.UUID;

@Singleton
public class RoutingLifecycle implements EventListener {
	private final RoutingService routingService;
	private final ConnectionStateRegistry connectionStateRegistry;
	private final EventManager eventManager;
	private final RoutingTargetApplier routingTargetApplier;

	@Inject
	public RoutingLifecycle(
			RoutingService routingService,
			ConnectionStateRegistry connectionStateRegistry,
			EventManager eventManager,
			RoutingTargetApplier routingTargetApplier
	) {
		this.routingService = routingService;
		this.connectionStateRegistry = connectionStateRegistry;
		this.eventManager = eventManager;
		this.routingTargetApplier = routingTargetApplier;
		eventManager.register(this);
	}

	@IdenticEvent
	public void onStepPrepare(StepPrepareEvent event) {
		if (event == null)
			return;

		AuthFlowType flow = event.getContext().get(IdenticaKeys.CURRENT_FLOW).orElse(null);
		if (flow == AuthFlowType.SEAMLESS)
			return;

		RoutingDecision decision = new RoutingDecision(
				event.getContext(),
				event.getPhase(),
				event.getStep(),
				StepResult.waiting("")
		);

		RoutingTarget target = routingService.resolve(decision).orElse(null);
		if (target == null) {
			clear(event.getContext());
			return;
		}

		store(event.getContext(), target);
	}

	@IdenticEvent
	public void onStepFinished(StepFinishedEvent event) {
		if (event == null || event.getContext() == null || event.getResult() == null)
			return;

		StepResult result = event.getResult();
		if (result.getStatus() == null)
			return;

		if (result.getStatus() == StepResult.StepStatus.WAITING)
			return;

		if (result.getStatus() != StepResult.StepStatus.COMPLETE) {
			clear(event.getContext());
			return;
		}

		RoutingDecision decision = new RoutingDecision(
				event.getContext(),
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
	}

	@IdenticEvent
	public void onPendingCleared(AuthPendingClearedEvent event) {
		if (event == null || event.getConnectionUniqueId() == null) return;
		connectionStateRegistry.find(event.getConnectionUniqueId())
				.ifPresent(ConnectionState::clearRoutingTarget);
	}

	private void store(AuthContext context, RoutingTarget target) {
		UUID connectionId = context != null ? context.getConnectionUniqueId() : null;
		if (connectionId == null || target == null) return;

		ConnectionState state = connectionStateRegistry.ensure(connectionId);
		state.putRoutingTarget(target);
		state.putContext(context);
		routingTargetApplier.apply(target, context);
		eventManager.call(new RoutingTargetUpdatedEvent(connectionId, target, context));
	}

	private void clear(AuthContext context) {
		UUID connectionId = context != null ? context.getConnectionUniqueId() : null;
		if (connectionId == null) return;

		connectionStateRegistry.find(connectionId)
				.ifPresent(ConnectionState::clearRoutingTarget);
	}

}
