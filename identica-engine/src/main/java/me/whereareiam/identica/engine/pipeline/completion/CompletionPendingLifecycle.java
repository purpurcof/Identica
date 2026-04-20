package me.whereareiam.identica.engine.pipeline.completion;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.whereareiam.identica.pipeline.completion.CompletionCoordinator;
import me.whereareiam.identica.model.pipeline.completion.CompletionPendingState;
import me.whereareiam.identica.pipeline.completion.CompletionPendingStore;
import me.whereareiam.identica.event.EventListener;
import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.event.base.IdenticEvent;
import me.whereareiam.identica.event.identity.IdentityAttachedEvent;
import me.whereareiam.identica.event.routing.RoutingTargetReachedEvent;
import me.whereareiam.identica.event.session.SessionOpenedEvent;
import me.whereareiam.identica.identity.IdentityService;
import me.whereareiam.identica.model.RoutingTarget;
import me.whereareiam.identica.routing.RoutingStateStore;
import me.whereareiam.identica.type.RoutingTargetType;
import org.jetbrains.annotations.NotNull;

@Singleton
public class CompletionPendingLifecycle implements EventListener {
	private final CompletionPendingStore completionPendingStore;
	private final CompletionCoordinator completionCoordinator;
	private final IdentityService identityService;
	private final RoutingStateStore routingStateStore;

	@Inject
	public CompletionPendingLifecycle(
			@NotNull CompletionPendingStore completionPendingStore,
			@NotNull CompletionCoordinator completionCoordinator,
			@NotNull IdentityService identityService,
			@NotNull RoutingStateStore routingStateStore,
			@NotNull EventManager eventManager
	) {
		this.completionPendingStore = completionPendingStore;
		this.completionCoordinator = completionCoordinator;
		this.identityService = identityService;
		this.routingStateStore = routingStateStore;
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
		if (hasRoutingTarget(event.getIdentity().getUniqueId())) return;

		completionCoordinator.consumeAndExecute(event.getIdentity());
	}

	@IdenticEvent
	public void onRoutingTargetReached(@NotNull RoutingTargetReachedEvent event) {
		RoutingTarget target = event.getTarget();
		if (target == null || target.getType() != RoutingTargetType.COMPLETED) return;

		identityService.find(event.getConnectionUniqueId())
				.ifPresent(completionCoordinator::consumeAndExecute);
	}

	private boolean hasRoutingTarget(@NotNull java.util.UUID connectionUniqueId) {
		RoutingTarget target = routingStateStore.peek(connectionUniqueId).orElse(null);
		return target != null
				&& target.getServer() != null
				&& !target.getServer().isBlank();
	}
}
