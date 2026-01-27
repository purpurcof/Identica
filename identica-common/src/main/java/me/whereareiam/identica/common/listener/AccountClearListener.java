package me.whereareiam.identica.common.listener;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.Serializer;
import me.whereareiam.identica.actor.Identity;
import me.whereareiam.identica.actor.OfflineIdentity;
import me.whereareiam.identica.auth.AuthCoordinator;
import me.whereareiam.identica.common.auth.handshake.HandshakeInstructionStore;
import me.whereareiam.identica.database.AccountPersistenceService;
import me.whereareiam.identica.database.ProviderLinkPersistenceService;
import me.whereareiam.identica.event.EventListener;
import me.whereareiam.identica.event.account.AccountClearEvent;
import me.whereareiam.identica.event.base.IdenticEvent;
import me.whereareiam.identica.model.config.Messages;
import me.whereareiam.identica.registry.IdentityRegistry;
import me.whereareiam.identica.type.ClearScope;

import java.util.Optional;
import java.util.UUID;

@Singleton
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class AccountClearListener implements EventListener {
	private final AccountPersistenceService accountPersistenceService;
	private final ProviderLinkPersistenceService providerLinkPersistenceService;
	private final HandshakeInstructionStore instructionStore;
	private final AuthCoordinator authCoordinator;
	private final IdentityRegistry identityRegistry;
	private final Provider<Messages> messagesProvider;

	@IdenticEvent
	public void onClear(AccountClearEvent event) {
		OfflineIdentity offlineIdentity = event.getIdentity();
		String username = offlineIdentity.getUsername();
		instructionStore.invalidate(username);

		UUID identicaUniqueId = offlineIdentity.getUniqueId();
		Optional<Identity> online = findOnlineIdentity(identicaUniqueId, username);
		online.ifPresent(identity -> {
			authCoordinator.clearPending(identity.getUniqueId());
			String disconnectMessage = joinLines(messagesProvider.get().getCommands().getClear().getDisconnect());
			if (!disconnectMessage.isBlank()) {
				identity.disconnect(Serializer.serialize(identity, disconnectMessage));
			}
		});

		if (event.getScope() == ClearScope.ALL) {
			if (identicaUniqueId == null) return;
			providerLinkPersistenceService.deleteAll(identicaUniqueId);
			accountPersistenceService.delete(identicaUniqueId);
		}
	}

	private Optional<Identity> findOnlineIdentity(UUID uniqueId, String username) {
		if (uniqueId != null) {
			Optional<Identity> byId = identityRegistry.findOnline(uniqueId);
			if (byId.isPresent()) return byId;
		}

		if (username == null || username.isBlank()) return Optional.empty();
		return identityRegistry.findOnline(username);
	}

	private String joinLines(java.util.List<String> lines) {
		if (lines == null || lines.isEmpty()) return "";
		return String.join("\n", lines);
	}
}


