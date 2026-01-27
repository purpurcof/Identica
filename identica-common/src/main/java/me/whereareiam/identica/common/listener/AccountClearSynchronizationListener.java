package me.whereareiam.identica.common.listener;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import me.whereareiam.identica.actor.OfflineIdentity;
import me.whereareiam.identica.cache.codec.type.JsonCodec;
import me.whereareiam.identica.event.EventListener;
import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.event.account.AccountClearEvent;
import me.whereareiam.identica.event.base.IdenticEvent;
import me.whereareiam.identica.model.config.Settings;
import me.whereareiam.identica.service.SynchronizationService;
import me.whereareiam.identica.type.ClearScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@Singleton
public class AccountClearSynchronizationListener implements EventListener {
	private static final JsonCodec<AccountClearMessage> CODEC = JsonCodec.of(AccountClearMessage.class);

	private final SynchronizationService synchronizationService;
	private final Provider<Settings> settingsProvider;
	private final EventManager eventManager;

	@Inject
	public AccountClearSynchronizationListener(
			SynchronizationService synchronizationService,
			Provider<Settings> settingsProvider,
			EventManager eventManager
	) {
		this.synchronizationService = synchronizationService;
		this.settingsProvider = settingsProvider;
		this.eventManager = eventManager;
		subscribe();
	}

	@IdenticEvent
	public void onAccountClear(AccountClearEvent event) {
		if (event == null || event.isSynchronizedEvent()) return;
		String channel = getAccountUpdatesChannel();
		if (channel.isBlank()) return;

		OfflineIdentity identity = event.getIdentity();
		String serverId = getServerId();
		UUID uniqueId = identity.getUniqueId();
		String username = identity.getUsername();
		byte[] payload = encode(
				serverId,
				event.getScope(),
				uniqueId,
				username
		);

		if (payload == null || payload.length == 0) return;
		synchronizationService.publish(channel, payload);
	}

	private void subscribe() {
		String channel = getAccountUpdatesChannel();
		if (channel.isBlank()) return;

		synchronizationService.subscribe(channel, payload -> {
			AccountClearMessage message = decode(payload);
			if (message == null) return;

			String localServerId = getServerId();
			if (!localServerId.isBlank() && localServerId.equalsIgnoreCase(message.serverId))
				return;

		if (message.username == null || message.username.isBlank()) return;
		OfflineIdentity identity = new OfflineIdentity(message.uniqueId, message.username, null);
			eventManager.call(new AccountClearEvent(identity, message.scope, true));
		});
	}

	private byte[] encode(String serverId, ClearScope scope, UUID uniqueId, String username) {
		if (scope == null || uniqueId == null) return null;

		AccountClearMessage message = new AccountClearMessage(
				serverId,
				scope,
				uniqueId,
				username
		);

		return CODEC.encode(message);
	}

	private AccountClearMessage decode(byte[] payload) {
		if (payload == null || payload.length == 0) return null;

		return CODEC.decode(payload);
	}

	private String getServerId() {
		return settingsProvider.get().getSynchronization().getServerId();
	}

	private String getAccountUpdatesChannel() {
		Settings.Synchronization sync = settingsProvider.get().getSynchronization();

		return sync.getRedis().getChannels().getAccountUpdates();
	}

	@NoArgsConstructor
	@AllArgsConstructor
	private static final class AccountClearMessage {
		private @Nullable String serverId;
		private @NotNull ClearScope scope;
		private @NotNull UUID uniqueId;
		private @Nullable String username;
	}
}
