package me.whereareiam.identica.provider.premium.command;

import com.google.inject.Inject;
import com.google.inject.Provider;
import me.whereareiam.identica.Serializer;
import me.whereareiam.identica.annotation.Argument;
import me.whereareiam.identica.annotation.Command;
import me.whereareiam.identica.annotation.Default;
import me.whereareiam.identica.annotation.Definition;
import me.whereareiam.identica.command.ProtectedActionCommand;
import me.whereareiam.identica.identity.actor.Identity;
import me.whereareiam.identica.identity.session.SessionService;
import me.whereareiam.identica.model.Session;
import me.whereareiam.identica.model.config.Messages;
import me.whereareiam.identica.model.migration.PendingMigration;
import me.whereareiam.identica.model.migration.operation.MigrationCancel;
import me.whereareiam.identica.model.migration.operation.MigrationConfirm;
import me.whereareiam.identica.model.migration.operation.MigrationRequest;
import me.whereareiam.identica.model.migration.operation.MigrationResult;
import me.whereareiam.identica.provider.ProviderManager;
import me.whereareiam.identica.provider.premium.PremiumConstants;
import me.whereareiam.identica.provider.premium.config.PremiumMessages;
import me.whereareiam.identica.service.MigrationService;
import me.whereareiam.identica.type.migration.MigrationCancelScope;
import me.whereareiam.identica.type.migration.MigrationInitiator;
import me.whereareiam.identica.type.migration.MigrationResultStatus;
import me.whereareiam.identica.type.provider.ProviderCapability;
import me.whereareiam.identica.verification.VerificationService;
import me.whereareiam.keystone.Actor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PremiumCommand extends ProtectedActionCommand<MigrationRequest> {
	private final MigrationService migrationService;
	private final ProviderManager providerManager;
	private final Provider<PremiumMessages> messagesProvider;
	private final Provider<Messages> coreMessagesProvider;
	private final SessionService sessionService;

	@Inject
	public PremiumCommand(
			MigrationService migrationService,
			ProviderManager providerManager,
			Provider<PremiumMessages> messagesProvider,
			Provider<Messages> coreMessagesProvider,
			VerificationService verificationService,
			SessionService sessionService
	) {
		super(verificationService);
		this.migrationService = migrationService;
		this.providerManager = providerManager;
		this.messagesProvider = messagesProvider;
		this.coreMessagesProvider = coreMessagesProvider;
		this.sessionService = sessionService;
	}

	@Override
	protected @NotNull SessionService sessionService() {
		return sessionService;
	}

	@Override
	protected @Nullable String currentSessionRequiredMessage() {
		return coreMessagesProvider.get().getCommands().getCurrentSessionRequired();
	}

	@Command("premium")
	@Definition("premium")
	public void onCommand(@NotNull Actor sender) {
		Identity identity = requireIdentity(sender, null);
		if (identity == null) return;
		Session session = requireCurrentSession(identity);
		if (session == null) return;
		if (!supportsMigration()) return;

		MigrationRequest request = MigrationRequest.builder()
				.connectionUniqueId(identity.getUniqueId())
				.identicaUniqueId(identity.getUniqueId())
				.targetProviderId(PremiumConstants.PROVIDER_ID)
				.username(identity.getUsername())
				.ip(identity.getIp())
				.initiator(MigrationInitiator.USER)
				.initiatorUniqueId(identity.getUniqueId())
				.build();

		MigrationResult result = migrationService.request(request);
		PremiumMessages.Commands.Premium messages = messagesProvider.get().getCommands().getPremium();
		if (result.getStatus() == MigrationResultStatus.PENDING_CONFIRMATION) {
			sendMessage(identity, requiresStepUp(identity.getUniqueId())
					? messages.getVerificationRequired()
					: joinMessage(messages.getConfirm()));
			return;
		}
		if (result.getStatus() == MigrationResultStatus.PENDING_EXISTS) {
			sendMessage(identity, messages.getPendingExists());
			return;
		}
		if (result.getStatus() == MigrationResultStatus.ALREADY_PRIMARY) {
			sendMessage(identity, messages.getAlreadyPrimary());
			return;
		}
		if (result.getStatus() == MigrationResultStatus.PRECHECK_DENIED && result.getMessage() != null) {
			sendMessage(identity, result.getMessage());
		}
	}

	@Command("premium confirm [input]")
	@Definition("premium-confirm")
	public void confirm(@NotNull Actor sender, @Argument("input") @Default("") String input) {
		Identity identity = requireIdentity(sender, null);
		if (identity == null) return;
		if (requireCurrentSession(identity) == null) return;

		PendingMigration pendingMigration = migrationService.findPendingMigration(identity.getUniqueId()).orElse(null);
		boolean verificationRequired = pendingMigration != null
				&& pendingMigration.getPhase() == PendingMigration.Phase.CONFIRMATION
				&& requiresStepUp(identity.getUniqueId());
		if (verificationRequired) {
			if (input.isBlank()) {
				sendMessage(identity, messagesProvider.get().getCommands().getPremium().getVerificationRequired());
				return;
			}

			StepUpResult result = confirmStepUp(identity.getUniqueId(), input, "migration-confirm");
			if (result.getStatus() != StepUpResult.Status.VERIFIED) {
				switch (result.getStatus()) {
					case INVALID_CODE -> sendMessage(identity, coreMessagesProvider.get().getCommands().getVerification().getConfirm().getInvalidCode());
					case CURRENT_SESSION_REQUIRED -> sendMessage(identity, coreMessagesProvider.get().getCommands().getVerification().getConfirm().getProtectedActionSessionRequired());
					case SELECTION_REQUIRED -> sendMessage(identity, coreMessagesProvider.get().getCommands().getVerification().getConfirm().getProtectedActionSelectionRequired());
					default -> sendMessage(identity, messagesProvider.get().getCommands().getPremium().getNoPending());
				}
				return;
			}
		}

		var result = migrationService.confirm(MigrationConfirm.builder()
				.connectionUniqueId(identity.getUniqueId())
				.kickMessage(joinMessage(resolveKickMessage()))
				.build());

		PremiumMessages.Commands.Premium messages = messagesProvider.get().getCommands().getPremium();
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
		if (result.getStatus() == MigrationResultStatus.PRECHECK_DENIED && result.getMessage() != null)
			sendMessage(identity, result.getMessage());
	}

	@Command("premium cancel")
	@Definition("premium-cancel")
	public void cancel(@NotNull Actor sender) {
		Identity identity = requireIdentity(sender, null);
		if (identity == null) return;
		if (requireCurrentSession(identity) == null) return;

		var result = migrationService.cancel(MigrationCancel.builder()
				.connectionUniqueId(identity.getUniqueId())
				.scope(MigrationCancelScope.CONFIRMATION)
				.build());

		PremiumMessages.Commands.Premium messages = messagesProvider.get().getCommands().getPremium();
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
						&& PremiumConstants.PROVIDER_ID.equalsIgnoreCase(provider.getDescriptor().getId()));
	}

	private List<String> resolveKickMessage() {
		PremiumMessages.Commands.Premium messages = messagesProvider.get().getCommands().getPremium();
		return messages.getConfirmed();
	}

	private void sendMessage(@NotNull Identity sender, @NotNull String message) {
		if (message.isBlank()) return;
		sender.sendMessage(Serializer.serialize(sender, message));
	}

	private String joinMessage(List<String> lines) {
		return lines == null ? "" : String.join("\n", lines);
	}
}
