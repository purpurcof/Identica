package me.whereareiam.identica.provider.premium.platform.bungeecord.listener.connection;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.listener.DynamicListener;
import me.whereareiam.identica.provider.ProviderAttemptStore;
import me.whereareiam.identica.provider.premium.PremiumConstants;
import me.whereareiam.identica.provider.premium.profile.PremiumProfileStore;
import me.whereareiam.identica.util.UniqueIdGenerator;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.event.LoginEvent;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.UUID;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class PremiumLoginListener implements DynamicListener<LoginEvent> {
	private final PremiumProfileStore profileStore;
	private final ProviderAttemptStore attemptStore;

	@Override
	public void onEvent(LoginEvent event) {
		PendingConnection connection = event.getConnection();
		if (connection == null) return;

		UUID profileId = connection.getUniqueId();
		if (profileId == null) return;

		String username = connection.getName();
		if (username == null || username.isBlank()) return;
		String ip = resolveIp(connection);

		UUID offlineUuid = UniqueIdGenerator.offlinePlayerUniqueId(username);
		if (offlineUuid != null && !profileId.equals(offlineUuid))
			attemptStore.clearAttempt(PremiumConstants.PROVIDER_ID, PremiumConstants.ATTEMPT_SCOPE_VERIFY, username, ip);

		profileStore.save(username, profileId.toString());
	}

	private String resolveIp(PendingConnection connection) {
		SocketAddress address = connection.getSocketAddress();
		if (!(address instanceof InetSocketAddress inetSocketAddress) || inetSocketAddress.getAddress() == null)
			return null;

		return inetSocketAddress.getAddress().getHostAddress();
	}
}
