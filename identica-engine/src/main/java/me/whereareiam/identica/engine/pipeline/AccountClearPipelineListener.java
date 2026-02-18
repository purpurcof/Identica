package me.whereareiam.identica.engine.pipeline;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.whereareiam.identica.event.EventListener;
import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.event.account.AccountClearEvent;
import me.whereareiam.identica.event.connection.ConnectionPendingClearedEvent;
import me.whereareiam.identica.event.base.IdenticEvent;
import me.whereareiam.identica.identity.IdentityService;
import me.whereareiam.identica.identity.actor.Identity;
import me.whereareiam.identica.pipeline.state.PipelineStateReference;
import me.whereareiam.identica.pipeline.state.PipelineStateStore;
import me.whereareiam.identica.type.event.EventOrder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@Singleton
public class AccountClearPipelineListener implements EventListener {
	private final PipelineStateStore pipelineStateStore;
	private final IdentityService identityService;
	private final EventManager eventManager;

	@Inject
	public AccountClearPipelineListener(
			@NotNull PipelineStateStore pipelineStateStore,
			@NotNull IdentityService identityService,
			@NotNull EventManager eventManager
	) {
		this.pipelineStateStore = pipelineStateStore;
		this.identityService = identityService;
		this.eventManager = eventManager;
		eventManager.register(this);
	}

	@IdenticEvent(EventOrder.LOW)
	public void onAccountClear(@NotNull AccountClearEvent event) {
		UUID connectionUniqueId = resolveConnectionUniqueId(event);
		String username = event.getIdentity().getUsername();

		PipelineStateReference reference = PipelineStateReference.builder()
				.connectionUniqueId(connectionUniqueId)
				.username(username)
				.build();
		if (reference.isEmpty()) return;

		boolean removed = pipelineStateStore.consume(reference).isPresent();
		if (connectionUniqueId != null) {
			eventManager.call(new ConnectionPendingClearedEvent(connectionUniqueId, removed));
		}
	}

	private @Nullable UUID resolveConnectionUniqueId(@NotNull AccountClearEvent event) {
		UUID connectionUniqueId = event.getIdentity().getUniqueId();
		if (connectionUniqueId != null) return connectionUniqueId;

		String username = event.getIdentity().getUsername();
		if (username.isBlank()) return null;

		return identityService.find(username)
				.map(Identity::getUniqueId)
				.orElse(null);
	}
}
