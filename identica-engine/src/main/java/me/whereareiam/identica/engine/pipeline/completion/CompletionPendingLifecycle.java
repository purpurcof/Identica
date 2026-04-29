package me.whereareiam.identica.engine.pipeline.completion;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.whereareiam.identica.event.EventListener;
import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.event.base.IdenticEvent;
import me.whereareiam.identica.event.identity.IdentityAttachedEvent;
import me.whereareiam.identica.event.routing.intent.RoutingIntentReachedEvent;
import me.whereareiam.identica.event.session.SessionOpenedEvent;
import me.whereareiam.identica.identity.IdentityService;
import me.whereareiam.identica.model.pipeline.completion.CompletionPendingState;
import me.whereareiam.identica.model.routing.RoutingIntent;
import me.whereareiam.identica.pipeline.completion.CompletionPendingStore;
import me.whereareiam.identica.routing.RoutingIntentStore;
import me.whereareiam.identica.type.routing.RoutingReason;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@Singleton
public class CompletionPendingLifecycle implements EventListener {
	private final CompletionPendingStore completionPendingStore;
	private final CompletionPipeline completionPipeline;
	private final IdentityService identityService;
	private final RoutingIntentStore routingIntentStore;

	@Inject
	public CompletionPendingLifecycle(
			@NotNull CompletionPendingStore completionPendingStore,
			@NotNull CompletionPipeline completionPipeline,
			@NotNull IdentityService identityService,
			@NotNull RoutingIntentStore routingIntentStore,
			@NotNull EventManager eventManager
	) {
		this.completionPendingStore = completionPendingStore;
		this.completionPipeline = completionPipeline;
		this.identityService = identityService;
		this.routingIntentStore = routingIntentStore;
		eventManager.register(this);
	}

	@IdenticEvent
	public void onSessionOpened(@NotNull SessionOpenedEvent event) {
		completionPendingStore.put(event.getConnectionUniqueId(), CompletionPendingState.builder()
				.pipelineType(event.getPipelineType())
				.connectionUniqueId(event.getConnectionUniqueId())
				.identicaUniqueId(event.getSession().getUniqueId())
				.sessionReused(event.isSessionReused())
				.build());
	}

	@IdenticEvent
	public void onIdentityAttached(@NotNull IdentityAttachedEvent event) {
		if (completionPendingStore.peek(event.getIdentity().getUniqueId()).isEmpty()) return;
		if (hasRoutingIntent(event.getIdentity().getUniqueId())) return;

		completionPipeline.complete(event.getIdentity());
	}

	@IdenticEvent
	public void onRoutingIntentReached(@NotNull RoutingIntentReachedEvent event) {
		RoutingIntent intent = event.getIntent();
		if (intent.getReason() != RoutingReason.COMPLETION) return;

		identityService.find(intent.getConnectionUniqueId())
				.ifPresent(completionPipeline::complete);
	}

	private boolean hasRoutingIntent(@NotNull UUID connectionUniqueId) {
		RoutingIntent intent = routingIntentStore.peek(connectionUniqueId).orElse(null);
		return intent != null && !intent.getEndpoint().getServer().isBlank();
	}
}
