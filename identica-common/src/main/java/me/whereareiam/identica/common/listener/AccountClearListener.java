package me.whereareiam.identica.common.listener;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import me.whereareiam.identica.Serializer;
import me.whereareiam.identica.auth.AuthenticationCoordinator;
import me.whereareiam.identica.common.auth.handshake.HandshakeInstructionRegistry;
import me.whereareiam.identica.database.AccountPersistenceService;
import me.whereareiam.identica.database.ProviderLinkPersistenceService;
import me.whereareiam.identica.event.EventListener;
import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.event.account.AccountClearEvent;
import me.whereareiam.identica.event.base.IdenticEvent;
import me.whereareiam.identica.identity.actor.ConnectionIdentity;
import me.whereareiam.identica.identity.actor.Identity;
import me.whereareiam.identica.model.config.Messages;
import me.whereareiam.identica.identity.IdentityService;
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
	private final @NotNull AuthenticationCoordinator authenticationCoordinator;
	private final @NotNull IdentityService identityService;
	private final @NotNull Provider<Messages> messagesProvider;

	@Inject
	public AccountClearListener(
			@NotNull AccountPersistenceService accountPersistenceService,
			@NotNull ProviderLinkPersistenceService providerLinkPersistenceService,
			@NotNull HandshakeInstructionRegistry instructionStore,
			@NotNull AuthenticationCoordinator authenticationCoordinator,
			@NotNull IdentityService identityService,
			@NotNull Provider<Messages> messagesProvider,
			@NotNull EventManager eventManager
	) {
		this.accountPersistenceService = accountPersistenceService;
		this.providerLinkPersistenceService = providerLinkPersistenceService;
		this.instructionStore = instructionStore;
		this.authenticationCoordinator = authenticationCoordinator;
		this.identityService = identityService;
		this.messagesProvider = messagesProvider;
		eventManager.register(this);
	}

	@IdenticEvent
	public void onClear(@NotNull AccountClearEvent event) {
		ConnectionIdentity ConnectionIdentity = event.getIdentity();
		String username = ConnectionIdentity.getUsername();
		instructionStore.invalidate(username);

		UUID identicaUniqueId = ConnectionIdentity.getUniqueId();
		Optional<Identity> player = findPlayerIdentity(identicaUniqueId, username);
		player.ifPresent(identity -> {
			authenticationCoordinator.clearPending(identity.getUniqueId());
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

	private @NotNull Optional<Identity> findPlayerIdentity(@Nullable UUID uniqueId, @Nullable String username) {
		if (uniqueId != null) {
			Optional<Identity> byId = identityService.find(uniqueId);
			if (byId.isPresent()) return byId;
		}

		if (username == null || username.isBlank()) return Optional.empty();
		return identityService.find(username);
	}

	private @NotNull String joinLines(@NotNull List<String> lines) {
		return String.join("\n", lines);
	}
}
