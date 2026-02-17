package me.whereareiam.identica.provider.cracked.command;

import com.google.inject.Inject;
import com.google.inject.Provider;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.annotation.Command;
import me.whereareiam.identica.annotation.Definition;
import me.whereareiam.identica.Serializer;
import me.whereareiam.identica.identity.actor.Identity;
import me.whereareiam.identica.migration.MigrationCancel;
import me.whereareiam.identica.migration.MigrationConfirm;
import me.whereareiam.identica.migration.MigrationRequest;
import me.whereareiam.identica.migration.MigrationResult;
import me.whereareiam.identica.migration.MigrationService;
import me.whereareiam.identica.provider.ProviderManager;
import me.whereareiam.identica.provider.cracked.CrackedConstants;
import me.whereareiam.identica.provider.cracked.config.CrackedMessages;
import me.whereareiam.identica.type.migration.MigrationCancelScope;
import me.whereareiam.identica.type.migration.MigrationInitiator;
import me.whereareiam.identica.type.migration.MigrationResultStatus;
import me.whereareiam.identica.type.provider.ProviderCapability;
import me.whereareiam.keystone.Actor;
import me.whereareiam.keystone.model.SerializerContent;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class CrackedCommand {
	private final Provider<CrackedMessages> messagesProvider;
	private final MigrationService migrationService;
	private final ProviderManager providerManager;

	@Definition("cracked")
	@Command("cracked")
	public void cracked(@NotNull Actor sender) {
		if (!(sender instanceof Identity identity))
			return;

		if (!supportsMigration())
			return;

		MigrationResult result = migrationService.request(MigrationRequest.builder()
				.connectionUniqueId(identity.getUniqueId())
				.targetProviderId(CrackedConstants.PROVIDER_ID)
				.username(identity.getUsername())
				.ip(identity.getIp())
				.initiator(MigrationInitiator.USER)
				.initiatorUniqueId(identity.getUniqueId())
				.build());

		CrackedMessages.Commands.Cracked messages = messagesProvider.get().getCommands().getCracked();
		if (result.getStatus() == MigrationResultStatus.PENDING_CONFIRMATION) {
			sendMessage(identity, joinMessage(messages.getConfirm()));
			return;
		}
		if (result.getStatus() == MigrationResultStatus.PENDING_EXISTS) {
			sendMessage(identity, messages.getPendingExists());
			return;
		}
		if (result.getStatus() == MigrationResultStatus.ALREADY_PRIMARY) {
			sendMessage(identity, messages.getAlreadyPrimary());
		}
	}

	@Definition("cracked-confirm")
	@Command("cracked confirm")
	public void confirm(@NotNull Actor sender) {
		if (!(sender instanceof Identity identity))
			return;

		CrackedMessages.Commands.Cracked messages = messagesProvider.get().getCommands().getCracked();
		MigrationResult result = migrationService.confirm(MigrationConfirm.builder()
				.connectionUniqueId(identity.getUniqueId())
				.kickMessage(joinMessage(messages.getConfirmed()))
				.build());

		if (result.getStatus() == MigrationResultStatus.NO_PENDING) {
			sendMessage(identity, messages.getNoPending());
			return;
		}
		if (result.getStatus() == MigrationResultStatus.EXPIRED) {
			sendMessage(identity, messages.getExpired());
			return;
		}
		if (result.getStatus() == MigrationResultStatus.ALREADY_PRIMARY) {
			sendMessage(identity, messages.getAlreadyPrimary());
			return;
		}
		if (result.getStatus() == MigrationResultStatus.PRECHECK_DENIED) {
			String message = result.getMessage();
			if (message != null && !message.isBlank())
				sendMessage(identity, message);
		}
	}

	@Definition("cracked-cancel")
	@Command("cracked cancel")
	public void cancel(@NotNull Actor sender) {
		if (!(sender instanceof Identity identity))
			return;

		MigrationResult result = migrationService.cancel(MigrationCancel.builder()
				.connectionUniqueId(identity.getUniqueId())
				.scope(MigrationCancelScope.CONFIRMATION)
				.build());

		CrackedMessages.Commands.Cracked messages = messagesProvider.get().getCommands().getCracked();
		if (result.getStatus() == MigrationResultStatus.CANCELLED) {
			sendMessage(identity, messages.getCancelled());
			return;
		}

		sendMessage(identity, messages.getNoPending());
	}

	private boolean supportsMigration() {
		return providerManager.findProviders(ProviderCapability.MIGRATION).stream()
				.anyMatch(provider -> provider != null
						&& provider.getDescriptor() != null
						&& CrackedConstants.PROVIDER_ID.equalsIgnoreCase(provider.getDescriptor().getId()));
	}

	private void sendMessage(@NotNull Identity identity, String message) {
		if (message == null || message.isBlank())
			return;
		SerializerContent content = SerializerContent.builder()
				.receiver(identity)
				.message(message)
				.build();
		identity.sendMessage(Serializer.serialize(content));
	}

	private String joinMessage(List<String> lines) {
		return lines == null ? "" : String.join("\n", lines);
	}
}
