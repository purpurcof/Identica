package me.whereareiam.identica.provider.premium.platform.bungeecord.listener.connection;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.listener.DynamicListener;
import me.whereareiam.identica.model.pipeline.prepare.decision.PrepareDecision;
import me.whereareiam.identica.model.provider.ProviderContext;
import me.whereareiam.identica.pipeline.prepare.PrepareStateStore;
import me.whereareiam.identica.provider.ProviderAttemptStore;
import me.whereareiam.identica.provider.premium.PremiumConstants;
import me.whereareiam.identica.provider.premium.profile.PremiumProfileStore;
import me.whereareiam.identica.util.UniqueIdGenerator;
import me.whereareiam.identica.util.UniqueIdUtil;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PostLoginEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Optional;
import java.util.UUID;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class PremiumPostLoginListener implements DynamicListener<PostLoginEvent> {
	private final PremiumProfileStore profileStore;
	private final ProviderAttemptStore attemptStore;
	private final PrepareStateStore prepareStateStore;

	@Override
	public void onEvent(PostLoginEvent event) {
		ProxiedPlayer player = event.getPlayer();
		if (player == null) return;

		String username = player.getName();
		if (username == null || username.isBlank()) return;
		String ip = resolveIp(player);
		String profileId = resolveProfileId(player).orElse(null);
		if (profileId == null || profileId.isBlank()) return;
		UUID resolvedProfileUniqueId = UniqueIdUtil.parseUniqueId(profileId);

		UUID offlineUuid = UniqueIdGenerator.offlinePlayerUniqueId(username);
		if (resolvedProfileUniqueId != null && offlineUuid != null && !resolvedProfileUniqueId.equals(offlineUuid))
			attemptStore.clearAttempt(PremiumConstants.PROVIDER_ID, PremiumConstants.ATTEMPT_SCOPE_VERIFY, username, ip);

		profileStore.save(username, profileId);
	}

	private @NotNull Optional<String> resolveProfileId(@NotNull ProxiedPlayer player) {
		PrepareDecision prepared = prepareStateStore.peek(player.getUniqueId()).orElse(null);
		if (prepared == null) prepared = preparedByConnectionKey(player).orElse(null);

		ProviderContext provider = prepared != null ? prepared.getProvider() : null;
		if (provider != null
				&& provider.getProviderSubject() != null
				&& !provider.getProviderSubject().isBlank()) {
			return Optional.of(provider.getProviderSubject());
		}

		UUID liveUniqueId = player.getUniqueId();
		return liveUniqueId != null
				? Optional.of(liveUniqueId.toString())
				: Optional.empty();
	}

	private @NotNull Optional<PrepareDecision> preparedByConnectionKey(@NotNull ProxiedPlayer player) {
		String connectionKey = connectionKey(player);
		if (connectionKey == null || connectionKey.isBlank())
			return Optional.empty();

		return prepareStateStore.peek(connectionKey);
	}

	private @Nullable String connectionKey(@NotNull ProxiedPlayer player) {
		String username = player.getName();
		if (username == null || username.isBlank()) return null;

		String ip = resolveIp(player);
		String host = "";
		String port = "";
		if (player.getPendingConnection() != null && player.getPendingConnection().getVirtualHost() != null) {
			host = player.getPendingConnection().getVirtualHost().getHostString();
			port = Integer.toString(player.getPendingConnection().getVirtualHost().getPort());
		}

		return String.join("|", username, ip == null ? "" : ip, host, port);
	}

	private String resolveIp(ProxiedPlayer player) {
		SocketAddress address = player.getSocketAddress();
		if (!(address instanceof InetSocketAddress inetSocketAddress) || inetSocketAddress.getAddress() == null)
			return null;

		return inetSocketAddress.getAddress().getHostAddress();
	}
}
