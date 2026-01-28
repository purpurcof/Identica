package me.whereareiam.identica.common.listener;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import me.whereareiam.identica.Serializer;
import me.whereareiam.identica.identity.actor.Identity;
import me.whereareiam.identica.identity.actor.OfflineIdentity;
import me.whereareiam.identica.auth.AuthCoordinator;
import me.whereareiam.identica.common.auth.handshake.HandshakeInstructionRegistry;
import me.whereareiam.identica.database.AccountPersistenceService;
import me.whereareiam.identica.database.ProviderLinkPersistenceService;
import me.whereareiam.identica.event.EventListener;
import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.event.account.AccountClearEvent;
import me.whereareiam.identica.event.base.IdenticEvent;
import me.whereareiam.identica.model.config.Messages;
import me.whereareiam.identica.registry.IdentityRegistry;
import me.whereareiam.identica.type.ClearScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Singleton
public class AccountClearListener implements EventListener {
	private final @NotNull AccountPersistenceService accountPersistenceService;
	private final @NotNull ProviderLinkPersistenceService providerLinkPersistenceService;
	private final @NotNull HandshakeInstructionRegistry instructionStore;
	private final @NotNull AuthCoordinator authCoordinator;
	private final @NotNull IdentityRegistry identityRegistry;
	private final @NotNull Provider<Messages> messagesProvider;

	@Inject
	public AccountClearListener(
			@NotNull AccountPersistenceService accountPersistenceService,
			@NotNull ProviderLinkPersistenceService providerLinkPersistenceService,
			@NotNull HandshakeInstructionRegistry instructionStore,
			@NotNull AuthCoordinator authCoordinator,
			@NotNull IdentityRegistry identityRegistry,
			@NotNull Provider<Messages> messagesProvider,
			@NotNull EventManager eventManager
	) {
		this.accountPersistenceService = accountPersistenceService;
		this.providerLinkPersistenceService = providerLinkPersistenceService;
		this.instructionStore = instructionStore;
		this.authCoordinator = authCoordinator;
		this.identityRegistry = identityRegistry;
		this.messagesProvider = messagesProvider;
		eventManager.register(this);
	}

	@IdenticEvent
	public void onClear(@NotNull AccountClearEvent event) {
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

	private @NotNull Optional<Identity> findOnlineIdentity(@Nullable UUID uniqueId, @Nullable String username) {
		if (uniqueId != null) {
			Optional<Identity> byId = identityRegistry.findOnline(uniqueId);
			if (byId.isPresent()) return byId;
		}

		if (username == null || username.isBlank()) return Optional.empty();
		return identityRegistry.findOnline(username);
	}

	private @NotNull String joinLines(@Nullable List<String> lines) {
		if (lines == null || lines.isEmpty()) return "";
		return String.join("\n", lines);
	}
}


