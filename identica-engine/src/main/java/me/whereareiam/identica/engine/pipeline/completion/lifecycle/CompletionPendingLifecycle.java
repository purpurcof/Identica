package me.whereareiam.identica.engine.pipeline.completion.lifecycle;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import me.whereareiam.identica.engine.pipeline.completion.runtime.CompletionPipeline;
import me.whereareiam.identica.event.EventListener;
import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.event.base.IdenticEvent;
import me.whereareiam.identica.event.delivery.DeliveryCheckpointReachedEvent;
import me.whereareiam.identica.event.routing.completion.CompletionRoutingReachedEvent;
import me.whereareiam.identica.event.session.SessionOpenedEvent;
import me.whereareiam.identica.identity.IdentityService;
import me.whereareiam.identica.identity.actor.Identity;
import me.whereareiam.identica.model.config.Routing;
import me.whereareiam.identica.model.delivery.DeliveryDispatchContext;
import me.whereareiam.identica.model.delivery.DeliveryPayload;
import me.whereareiam.identica.model.delivery.DeliveryRequest;
import me.whereareiam.identica.model.delivery.DeliveryTarget;
import me.whereareiam.identica.model.pipeline.completion.CompletionPendingState;
import me.whereareiam.identica.service.DeliveryService;
import me.whereareiam.identica.service.PlatformDeliveryAdapter;
import me.whereareiam.identica.type.messaging.DeliveryCheckpoint;
import me.whereareiam.identica.type.messaging.DeliverySemantics;
import me.whereareiam.identica.type.messaging.DeliverySource;
import me.whereareiam.identica.type.pipeline.PipelineType;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Singleton
public class CompletionPendingLifecycle implements EventListener {
	private final DeliveryService deliveryService;
	private final CompletionPipeline completionPipeline;
	private final IdentityService identityService;
	private final PlatformDeliveryAdapter platformDeliveryAdapter;
	private final Provider<Routing> routingProvider;

	@Inject
	public CompletionPendingLifecycle(
			@NotNull DeliveryService deliveryService,
			@NotNull CompletionPipeline completionPipeline,
			@NotNull IdentityService identityService,
			@NotNull PlatformDeliveryAdapter platformDeliveryAdapter,
			@NotNull Provider<Routing> routingProvider,
			@NotNull EventManager eventManager
	) {
		this.deliveryService = deliveryService;
		this.completionPipeline = completionPipeline;
		this.identityService = identityService;
		this.platformDeliveryAdapter = platformDeliveryAdapter;
		this.routingProvider = routingProvider;
		eventManager.register(this);
	}

	@IdenticEvent
	public void onSessionOpened(@NotNull SessionOpenedEvent event) {
		String completionTarget = resolveCompletionTarget(event.getPipelineType());
		deliveryService.queue(DeliveryRequest.builder()
				.id(java.util.UUID.randomUUID())
				.source(DeliverySource.COMPLETION)
				.target(DeliveryTarget.builder()
						.connectionUniqueId(event.getConnectionUniqueId())
						.accountUniqueId(event.getSession().getUniqueId())
						.build())
				.payload(DeliveryPayload.builder()
						.completion(DeliveryPayload.CompletionPayload.builder()
								.connectionUniqueId(event.getConnectionUniqueId())
								.accountUniqueId(event.getSession().getUniqueId())
								.pipelineType(event.getPipelineType())
								.build())
						.build())
				.checkpoint(DeliveryCheckpoint.PLATFORM_READY_INITIAL)
				.semantics(DeliverySemantics.ONCE)
				.requiredServer(completionTarget)
				.createdAt(System.currentTimeMillis())
				.updatedAt(System.currentTimeMillis())
				.build());
	}

	@IdenticEvent
	public void onDeliveryCheckpointReached(@NotNull DeliveryCheckpointReachedEvent event) {
		dispatchCompletion(event.getIdentity(), event.getCheckpoint(), event.getCurrentServer());
	}

	@IdenticEvent
	public void onCompletionRoutingReached(@NotNull CompletionRoutingReachedEvent event) {
		identityService.findByConnectionUniqueId(event.getIntent().getConnectionUniqueId())
				.ifPresent(identity -> platformDeliveryAdapter.armInitialReady(identity, event.getCurrentServer()));
	}

	private void dispatchCompletion(
			@NotNull Identity identity,
			@NotNull DeliveryCheckpoint checkpoint,
			String currentServer
	) {
		List<DeliveryRequest> delivered = deliveryService.dispatch(DeliveryDispatchContext.builder()
				.checkpoint(checkpoint)
				.identity(identity)
				.currentServer(currentServer)
				.build());
		for (DeliveryRequest request : delivered) {
			DeliveryPayload.CompletionPayload completion = request.getPayload().getCompletion();
			if (completion == null) continue;

			completionPipeline.complete(identity, CompletionPendingState.builder()
					.pipelineType(completion.getPipelineType())
					.connectionUniqueId(completion.getConnectionUniqueId())
					.accountUniqueId(completion.getAccountUniqueId())
					.build());
			deliveryService.acknowledge(request.getId(), "completion-dispatched");
		}
	}

	private String resolveCompletionTarget(@NotNull PipelineType pipelineType) {
		Routing routing = routingProvider.get();
		Routing.Target target = routing.getDefaults().getComplete();
		Routing.Targets scenarioTargets = resolveScenarioTargets(routing, pipelineType);
		Routing.Target scenarioTarget = scenarioTargets != null ? scenarioTargets.getComplete() : null;
		if (scenarioTarget != null && !isBlank(scenarioTarget.getTarget()))
			return scenarioTarget.getTarget();

		return target.getTarget();
	}

	private Routing.Targets resolveScenarioTargets(
			@NotNull Routing routing,
			@NotNull PipelineType pipelineType
	) {
		String scenarioId = pipelineType.name().toLowerCase(Locale.ROOT);
		for (Map.Entry<String, Routing.Targets> entry : routing.getScenarios().entrySet()) {
			if (entry.getKey() == null || entry.getValue() == null) continue;
			if (entry.getKey().equalsIgnoreCase(scenarioId))
				return entry.getValue();
		}

		return null;
	}

	private boolean isBlank(String value) {
		return value == null || value.isBlank();
	}
}
