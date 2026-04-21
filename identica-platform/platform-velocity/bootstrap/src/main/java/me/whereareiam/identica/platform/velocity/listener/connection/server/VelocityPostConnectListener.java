package me.whereareiam.identica.platform.velocity.listener.connection.server;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.listener.DynamicListener;
import me.whereareiam.identica.platform.velocity.adapter.auth.VelocityResumeDecisionAdapter;
import me.whereareiam.identica.platform.velocity.adapter.routing.VelocityRoutingIntentReachedEmitter;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class VelocityPostConnectListener implements DynamicListener<ServerPostConnectEvent> {
	private final VelocityResumeDecisionAdapter resumeDecisionAdapter;
	private final VelocityRoutingIntentReachedEmitter reachedEmitter;

	@Override
	public void onEvent(ServerPostConnectEvent event) {
		resumeDecisionAdapter.onEvent(event);
		reachedEmitter.emitIfReached(event.getPlayer());
	}
}
