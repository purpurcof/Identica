package me.whereareiam.identica.engine.completion;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.whereareiam.identica.pipeline.completion.CompletionCoordinator;
import me.whereareiam.identica.model.pipeline.completion.CompletionPendingState;
import me.whereareiam.identica.pipeline.completion.CompletionPendingStore;
import me.whereareiam.identica.event.EventListener;
import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.event.base.IdenticEvent;
import me.whereareiam.identica.event.identity.IdentityAttachedEvent;
import me.whereareiam.identica.event.session.SessionOpenedEvent;
import me.whereareiam.identica.identity.IdentityService;
import org.jetbrains.annotations.NotNull;

@Singleton
public class CompletionPendingLifecycle implements EventListener {
	private final CompletionPendingStore completionPendingStore;
	private final CompletionCoordinator completionCoordinator;
	private final IdentityService identityService;

	@Inject
	public CompletionPendingLifecycle(
			@NotNull CompletionPendingStore completionPendingStore,
			@NotNull CompletionCoordinator completionCoordinator,
			@NotNull IdentityService identityService,
			@NotNull EventManager eventManager
	) {
		this.completionPendingStore = completionPendingStore;
		this.completionCoordinator = completionCoordinator;
		this.identityService = identityService;
		eventManager.register(this);
	}

	@IdenticEvent
	public void onSessionOpened(@NotNull SessionOpenedEvent event) {
		var identity = identityService.find(event.getConnectionUniqueId()).orElse(null);
		if (identity != null) {
			completionCoordinator.execute(identity, event.getPipelineType(), event.getSession(), event.isSessionReused());
			return;
		}

		completionPendingStore.put(event.getConnectionUniqueId(), CompletionPendingState.builder()
				.pipelineType(event.getPipelineType())
				.connectionUniqueId(event.getConnectionUniqueId())
				.identicaUniqueId(event.getSession().getUniqueId())
				.sessionReused(event.isSessionReused())
				.build());
	}

	@IdenticEvent
	public void onIdentityAttached(@NotNull IdentityAttachedEvent event) {
		completionCoordinator.consumeAndExecute(event.getIdentity());
	}
}
