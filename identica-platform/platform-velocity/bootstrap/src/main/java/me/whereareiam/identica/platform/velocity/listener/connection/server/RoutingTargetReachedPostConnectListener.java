package me.whereareiam.identica.platform.velocity.listener.connection.server;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.listener.DynamicListener;
import me.whereareiam.identica.model.scheduler.DelayedRunnableTask;
import me.whereareiam.identica.model.scheduler.JobKey;
import me.whereareiam.identica.model.scheduler.Origin;
import me.whereareiam.identica.model.scheduler.Purpose;
import me.whereareiam.identica.platform.velocity.adapter.routing.VelocityRoutingTargetReachedEmitter;
import me.whereareiam.identica.service.Scheduler;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class RoutingTargetReachedPostConnectListener implements DynamicListener<ServerPostConnectEvent> {
	private static final Origin ORIGIN = Origin.core(RoutingTargetReachedPostConnectListener.class);
	private static final Purpose PURPOSE = Purpose.of("routing-target-reached");
	private static final long EMIT_DELAY_MS = 50L;

	private final Scheduler scheduler;
	private final VelocityRoutingTargetReachedEmitter reachedEmitter;

	@Override
	public void onEvent(ServerPostConnectEvent event) {
		var player = event.getPlayer();
		scheduler.schedule(DelayedRunnableTask.builder()
				.key(JobKey.of(ORIGIN, PURPOSE, player.getUniqueId().toString()))
				.delay(EMIT_DELAY_MS)
				.runnable(() -> reachedEmitter.emitIfReached(player))
				.build());
	}
}
