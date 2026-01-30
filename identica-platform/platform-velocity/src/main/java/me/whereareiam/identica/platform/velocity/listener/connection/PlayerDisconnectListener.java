package me.whereareiam.identica.platform.velocity.listener.connection;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.proxy.Player;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.listener.DynamicListener;
import me.whereareiam.identica.registry.ConnectionStateRegistry;
import me.whereareiam.identica.registry.IdentityRegistry;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class PlayerDisconnectListener implements DynamicListener<DisconnectEvent> {
	private final ConnectionStateRegistry connectionStateRegistry;
	private final IdentityRegistry identityRegistry;

	@Override
	public void onEvent(DisconnectEvent event) {
		Player player = event.getPlayer();
		if (player == null) return;

		connectionStateRegistry.clear(player.getUniqueId());
		identityRegistry.removePlayer(player.getUniqueId());
	}
}
