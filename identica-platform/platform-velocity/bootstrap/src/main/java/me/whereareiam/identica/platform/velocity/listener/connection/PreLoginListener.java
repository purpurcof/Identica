package me.whereareiam.identica.platform.velocity.listener.connection;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.velocitypowered.api.event.AwaitingEventExecutor;
import com.velocitypowered.api.event.EventTask;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.platform.velocity.adapter.auth.VelocityHandshakeDecisionAdapter;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class PreLoginListener implements AwaitingEventExecutor<PreLoginEvent> {
	private final VelocityHandshakeDecisionAdapter handshakeDecisionAdapter;

	@Override
	public EventTask executeAsync(PreLoginEvent event) {
		if (!event.getResult().isAllowed()) return null;
		return EventTask.resumeWhenComplete(handshakeDecisionAdapter.process(event).toCompletableFuture());
	}
}
