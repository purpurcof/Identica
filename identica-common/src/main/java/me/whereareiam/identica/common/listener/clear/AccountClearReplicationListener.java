package me.whereareiam.identica.common.listener.clear;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import me.whereareiam.identica.event.EventListener;
import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.event.account.AccountClearEvent;
import me.whereareiam.identica.event.base.IdenticEvent;
import me.whereareiam.identica.identity.actor.ConnectionIdentity;
import me.whereareiam.identica.logging.Logger;
import me.whereareiam.identica.model.config.Replication;
import me.whereareiam.identica.replication.ReplicationChannel;
import me.whereareiam.identica.replication.ReplicationSystem;
import me.whereareiam.identica.model.replication.ReplicationType;
import me.whereareiam.identica.type.ClearScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@Singleton
public class AccountClearReplicationListener implements EventListener {
	private final @NotNull ReplicationSystem replicationSystem;
	private final @NotNull Provider<Replication> replicationProvider;
	private final @NotNull EventManager eventManager;
	private ReplicationChannel<AccountClearMessage> channel;

	@Inject
	public AccountClearReplicationListener(
			@NotNull ReplicationSystem replicationSystem,
			@NotNull Provider<Replication> replicationProvider,
			@NotNull EventManager eventManager
	) {
		this.replicationSystem = replicationSystem;
		this.replicationProvider = replicationProvider;
		this.eventManager = eventManager;
		eventManager.register(this);
		initializeChannel();
	}

	@IdenticEvent
	public void onAccountClear(@NotNull AccountClearEvent event) {
		if (event.isSynchronizedEvent()) return;
		if (channel == null) return;

		ConnectionIdentity identity = event.getIdentity();
		String serverId = getServerId();
		UUID uniqueId = identity.getUniqueId();
		String username = identity.getUsername();
		AccountClearMessage message = new AccountClearMessage(
				serverId,
				event.getScope(),
				uniqueId,
				username
		);
		channel.publish(message);
	}

	private void initializeChannel() {
		String channelName = getAccountUpdatesChannel();
		if (channelName.isBlank()) return;
		ReplicationType<AccountClearMessage, AccountClearMessage> type = ReplicationType.identity(AccountClearMessage.class);
		this.channel = replicationSystem.channel(channelName, type);
		this.channel.subscribe(message -> {
			String localServerId = getServerId();
			if (!localServerId.isBlank() && localServerId.equalsIgnoreCase(message.serverId))
				return;

			Logger.debug("Received synchronized account clear (scope: %s, user: %s)",
					message.scope,
					message.username != null ? message.username : message.uniqueId);

			if (message.username == null || message.username.isBlank()) return;
			ConnectionIdentity identity = new ConnectionIdentity(message.uniqueId, message.username, null);
			eventManager.call(new AccountClearEvent(identity, message.scope, true));
		});
	}

	private @NotNull String getServerId() {
		return replicationProvider.get().getServerId();
	}

	private @NotNull String getAccountUpdatesChannel() {
		Replication replication = replicationProvider.get();
		return replication.getRedis().getChannels().getAccountUpdates();
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
