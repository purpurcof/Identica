package me.whereareiam.identica.platform.velocity.adapter.profile;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.velocitypowered.api.event.player.GameProfileRequestEvent;
import com.velocitypowered.api.util.GameProfile;
import me.whereareiam.identica.auth.AuthenticationCoordinator;
import me.whereareiam.identica.auth.adapter.ProfileRewriteAdapter;
import me.whereareiam.identica.identity.actor.ConnectionIdentity;
import me.whereareiam.identica.listener.DynamicListener;
import me.whereareiam.identica.provider.ProviderManager;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@Singleton
public class VelocityProfileRewriteAdapter extends ProfileRewriteAdapter implements DynamicListener<GameProfileRequestEvent> {
	@Inject
	public VelocityProfileRewriteAdapter(
			@NotNull ProviderManager providerManager,
			@NotNull AuthenticationCoordinator authenticationCoordinator
	) {
		super(providerManager, authenticationCoordinator);
	}

	@Override
	public void onEvent(GameProfileRequestEvent event) {
		GameProfile current = event.getGameProfile();
		if (current == null) return;

		String ip = null;
		if (event.getConnection().getRemoteAddress() != null
				&& event.getConnection().getRemoteAddress().getAddress() != null) {
			ip = event.getConnection().getRemoteAddress().getAddress().getHostAddress();
		}
		ConnectionIdentity identity = new ConnectionIdentity(event.getUsername(), ip);

		UUID identicaUuid = resolveIdenticaUniqueId(identity);
		if (identicaUuid == null) return;
		if (current.getId() != null && current.getId().equals(identicaUuid)) return;

		event.setGameProfile(current.withId(identicaUuid));
	}

}
