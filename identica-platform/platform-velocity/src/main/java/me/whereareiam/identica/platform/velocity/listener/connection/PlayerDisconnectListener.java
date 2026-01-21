package me.whereareiam.identica.platform.velocity.listener.connection;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.proxy.Player;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.auth.AuthCoordinator;
import me.whereareiam.identica.listener.DynamicListener;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class PlayerDisconnectListener implements DynamicListener<DisconnectEvent> {
	private final AuthCoordinator authCoordinator;

	@Override
	public void onEvent(DisconnectEvent event) {
		Player player = event.getPlayer();
		if (player == null) return;

		authCoordinator.clearPending(player.getUniqueId());
	}
}
