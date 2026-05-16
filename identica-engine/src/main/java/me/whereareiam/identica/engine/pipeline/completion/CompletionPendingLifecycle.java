package me.whereareiam.identica.engine.pipeline.completion;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import me.whereareiam.identica.event.EventListener;
import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.event.base.IdenticEvent;
import me.whereareiam.identica.event.delivery.DeliveryCheckpointReachedEvent;
import me.whereareiam.identica.event.session.SessionOpenedEvent;
import me.whereareiam.identica.identity.actor.Identity;
import me.whereareiam.identica.model.config.Settings;
import me.whereareiam.identica.model.delivery.DeliveryDispatchContext;
import me.whereareiam.identica.model.delivery.DeliveryPayload;
import me.whereareiam.identica.model.delivery.DeliveryRequest;
import me.whereareiam.identica.model.delivery.DeliveryTarget;
import me.whereareiam.identica.model.pipeline.completion.CompletionPendingState;
import me.whereareiam.identica.service.DeliveryService;
import me.whereareiam.identica.type.messaging.DeliveryCheckpoint;
import me.whereareiam.identica.type.messaging.DeliverySemantics;
import me.whereareiam.identica.type.messaging.DeliverySource;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Singleton
public class CompletionPendingLifecycle implements EventListener {
	private final DeliveryService deliveryService;
	private final CompletionPipeline completionPipeline;
	private final Provider<Settings> settingsProvider;

	@Inject
	public CompletionPendingLifecycle(
			@NotNull DeliveryService deliveryService,
			@NotNull CompletionPipeline completionPipeline,
			@NotNull Provider<Settings> settingsProvider,
			@NotNull EventManager eventManager
	) {
		this.deliveryService = deliveryService;
		this.completionPipeline = completionPipeline;
		this.settingsProvider = settingsProvider;
		eventManager.register(this);
	}

	@IdenticEvent
	public void onSessionOpened(@NotNull SessionOpenedEvent event) {
		String completionTarget = resolveCompletionTarget();
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
								.authenticationRecognized(event.isAuthenticationRecognized())
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
					.authenticationRecognized(completion.isAuthenticationRecognized())
					.build());
			deliveryService.acknowledge(request.getId(), "completion-dispatched");
		}
	}

	private String resolveCompletionTarget() {
		try {
			return settingsProvider.get().getConnection().getRouting().getDefaults().getComplete().getTarget();
		} catch (Exception ignored) {
			return null;
		}
	}
}
