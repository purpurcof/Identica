package me.whereareiam.identica.common.routing.lifecycle;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.whereareiam.identica.auth.step.AuthenticationStep;
import me.whereareiam.identica.event.EventListener;
import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.event.auth.AuthPendingClearedEvent;
import me.whereareiam.identica.event.auth.step.AuthStepFinishedEvent;
import me.whereareiam.identica.event.base.IdenticEvent;
import me.whereareiam.identica.event.routing.RoutingTargetUpdatedEvent;
import me.whereareiam.identica.loader.IdenticaProvider;
import me.whereareiam.identica.loader.ProviderManager;
import me.whereareiam.identica.model.auth.AuthContext;
import me.whereareiam.identica.model.auth.StepResult;
import me.whereareiam.identica.model.provider.InternalProvider;
import me.whereareiam.identica.model.RoutingTarget;
import me.whereareiam.identica.routing.RoutingService;
import me.whereareiam.identica.routing.RoutingStateStore;
import me.whereareiam.identica.type.AuthStepType;

import java.util.List;
import java.util.UUID;

@Singleton
public class RountingListener implements EventListener {
	private final RoutingService routingService;
	private final RoutingStateStore routingStateStore;
	private final ProviderManager providerManager;
	private final EventManager eventManager;
	private final RoutingTargetDispatcher routingTargetDispatcher;

	@Inject
	public RountingListener(
			RoutingService routingService,
			RoutingStateStore routingStateStore,
			ProviderManager providerManager,
			EventManager eventManager,
			RoutingTargetDispatcher routingTargetDispatcher
	) {
		this.routingService = routingService;
		this.routingStateStore = routingStateStore;
		this.providerManager = providerManager;
		this.eventManager = eventManager;
		this.routingTargetDispatcher = routingTargetDispatcher;
		eventManager.register(this);
	}

	@IdenticEvent
	public void onStepFinished(AuthStepFinishedEvent event) {
		if (event == null) return;

		AuthContext context = event.getContext();
		StepResult result = event.getResult();
		if (context == null || result == null || result.getStatus() == null) {
			clear(context);
			return;
		}

		switch (result.getStatus()) {
			case WAITING -> handleWaiting(event, context);
			case COMPLETE -> handleComplete(context);
			case FAILED, DENIED, REQUIRE_RECONNECT, NO_PENDING -> clear(context);
			default -> {
			}
		}
	}

	@IdenticEvent
	public void onPendingCleared(AuthPendingClearedEvent event) {
		if (event == null || event.getConnectionUniqueId() == null) return;
		routingStateStore.clear(event.getConnectionUniqueId());
	}

	private void handleWaiting(AuthStepFinishedEvent event, AuthContext context) {
		AuthenticationStep step = event.getStep();
		if (step == null || step.getType() != AuthStepType.INTERACTIVE) {
			clear(context);
			return;
		}

		String providerId = resolveProviderId(event.getProvider());
		RoutingTarget target = routingService.resolveStepTarget(context, providerId, step).orElse(null);
		if (target == null) {
			clear(context);
			return;
		}

		store(context, target);
	}

	private void handleComplete(AuthContext context) {
		RoutingTarget target = routingService.resolveCompletionTarget(context).orElse(null);
		if (target == null) {
			clear(context);
			return;
		}

		store(context, target);
	}

	private void store(AuthContext context, RoutingTarget target) {
		UUID connectionId = context != null ? context.getConnectionUniqueId() : null;
		if (connectionId == null || target == null) return;

		routingStateStore.put(connectionId, target);
		if (routingTargetDispatcher != null) {
			routingTargetDispatcher.apply(target, context);
		}
		eventManager.call(new RoutingTargetUpdatedEvent(connectionId, target, context));
	}

	private void clear(AuthContext context) {
		UUID connectionId = context != null ? context.getConnectionUniqueId() : null;
		if (connectionId == null) return;

		routingStateStore.clear(connectionId);
	}

	private String resolveProviderId(IdenticaProvider provider) {
		if (provider == null) return null;

		List<InternalProvider> providers = providerManager.getProviders();
		if (providers == null) return null;

		for (InternalProvider internal : providers) {
			if (internal != null && internal.getProvider() == provider && internal.getDescriptor() != null) {
				String id = internal.getDescriptor().getId();
				if (id != null && !id.isBlank()) return id;
			}
		}

		return null;
	}
}
