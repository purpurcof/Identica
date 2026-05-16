package me.whereareiam.identica.common.messaging;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.whereareiam.identica.event.EventListener;
import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.event.base.IdenticEvent;
import me.whereareiam.identica.event.delivery.DeliveryCheckpointReachedEvent;
import me.whereareiam.identica.event.identity.IdentityAttachedEvent;
import me.whereareiam.identica.event.pipeline.state.PipelineStateClearedEvent;
import me.whereareiam.identica.event.routing.intent.RoutingIntentClearedEvent;
import me.whereareiam.identica.event.routing.intent.RoutingIntentReachedEvent;
import me.whereareiam.identica.identity.IdentityService;
import me.whereareiam.identica.identity.actor.Identity;
import me.whereareiam.identica.model.delivery.DeliveryDispatchContext;
import me.whereareiam.identica.model.delivery.DeliveryRequest;
import me.whereareiam.identica.service.DeliveryService;
import me.whereareiam.identica.type.messaging.DeliveryCheckpoint;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@Singleton
public class DeliveryLifecycle implements EventListener {
	private final DeliveryService deliveryService;
	private final IdentityService identityService;

	@Inject
	public DeliveryLifecycle(
			@NotNull DeliveryService deliveryService,
			@NotNull IdentityService identityService,
			@NotNull EventManager eventManager
	) {
		this.deliveryService = deliveryService;
		this.identityService = identityService;
		eventManager.register(this);
	}

	@IdenticEvent
	public void onIdentityAttached(@NotNull IdentityAttachedEvent event) {
		dispatch(event.getIdentity(), DeliveryCheckpoint.IDENTITY_ATTACHED, null);
	}

	@IdenticEvent
	public void onRoutingIntentReached(@NotNull RoutingIntentReachedEvent event) {
		UUID connectionUniqueId = event.getIntent().getConnectionUniqueId();

        identityService.findByConnectionUniqueId(connectionUniqueId)
				.ifPresent(identity -> dispatch(identity, DeliveryCheckpoint.ROUTING_REACHED, event.getCurrentServer()));
	}

	@IdenticEvent
	public void onDeliveryCheckpointReached(@NotNull DeliveryCheckpointReachedEvent event) {
		dispatch(event.getIdentity(), event.getCheckpoint(), event.getCurrentServer());
	}

	@IdenticEvent
	public void onRoutingIntentCleared(@NotNull RoutingIntentClearedEvent event) {
		for (DeliveryRequest request : deliveryService.pendingForConnection(event.getConnectionUniqueId())) {
			if (request.getCheckpoint() != DeliveryCheckpoint.ROUTING_REACHED) continue;

			deliveryService.acknowledge(request.getId(), "routing-intent-cleared");
		}
	}

	@IdenticEvent
	public void onPipelineStateCleared(@NotNull PipelineStateClearedEvent event) {
		UUID connectionUniqueId = event.getReference().getConnectionUniqueId();
		if (connectionUniqueId != null) deliveryService.invalidateByConnection(connectionUniqueId, "pipeline-state-cleared");
	}

	private void dispatch(
			@NotNull Identity identity,
			@NotNull DeliveryCheckpoint checkpoint,
			String currentServer
	) {
		deliveryService.dispatch(DeliveryDispatchContext.builder()
				.checkpoint(checkpoint)
				.identity(identity)
				.currentServer(currentServer)
				.build());
	}
}
