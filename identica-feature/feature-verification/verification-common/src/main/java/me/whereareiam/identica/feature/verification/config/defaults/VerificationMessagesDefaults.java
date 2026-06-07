package me.whereareiam.identica.feature.verification.config.defaults;

import com.google.inject.Singleton;
import me.whereareiam.configura.merge.defaults.DefaultsProvider;
import me.whereareiam.identica.feature.verification.config.VerificationMessages;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Singleton
public class VerificationMessagesDefaults implements DefaultsProvider<VerificationMessages> {
	@Override
	public VerificationMessages supply(@NotNull VerificationMessages messages) {
		VerificationMessages.Commands commands = new VerificationMessages.Commands();
		VerificationMessages.Commands.Admin admin = new VerificationMessages.Commands.Admin();
		VerificationMessages.Commands.Admin.Reset reset = new VerificationMessages.Commands.Admin.Reset();
		reset.setTargetNotFound("{prefix}<white>No account found for <gray>{target}</gray>.</white>");
		reset.setCompleted("{prefix}<white>Verification state reset for <gray>{target}</gray>.</white>");
		admin.setReset(reset);
		commands.setAdmin(admin);

		commands.setPlayerOnly("{prefix}<white>This command can only be used by a player.</white>");
		commands.setNotAllowed("{prefix}<white>This verification action is not allowed.</white>");

		VerificationMessages.Commands.Status status = new VerificationMessages.Commands.Status();
		status.setBody(List.of(
				" ",
				" <green><bold>Identica</bold>",
				"  <white>Here you can see all enrolled 2FA methods</white>",
				"  <white>and to which providers they are assigned.</white>",
				" ",
				"  <white>Enrolled methods:</white>",
				"{enrollments}",
				" ",
				"  <white>Provider selections:</white>",
				"{selections}",
				" "
		));
		status.setEmptyEnrollments("  <dark_gray>▪</dark_gray> <gray>No enrolled methods</gray>");
		status.setEmptySelections("  <dark_gray>▪</dark_gray> <gray>No provider selections</gray>");

		VerificationMessages.Commands.Enroll enroll = new VerificationMessages.Commands.Enroll();
		enroll.setUnknownMethod("{prefix}<white>Unknown verification method <gray>{methodDisplayName}</gray>.</white>");
		enroll.setAlreadyEnrolled("{prefix}<white>Method <gray>{methodDisplayName}</gray> is already enrolled.</white>");

		VerificationMessages.Commands.Confirm confirm = new VerificationMessages.Commands.Confirm();
		confirm.setNoPending("{prefix}<white>No pending verification action.</white>");
		confirm.setInvalidCode("{prefix}<white>Invalid verification code.</white>");
		confirm.setProtectedActionSelectionRequired("{prefix}<white>Select a verification method for your current provider before continuing.</white>");
		confirm.setProtectedActionSessionRequired("{prefix}<white>You must have an <red>active session</red> to confirm this protected action.</white>");
		confirm.setMethodUnavailable("{prefix}<white>Method <gray>{methodDisplayName}</gray> is not available for <gray>{provider}</gray>.</white>");
		confirm.setEnabled("{prefix}<white>Verification method <green>{methodDisplayName}</green> enabled.</white>");
		confirm.setAutoSelected("{prefix}<white>Verification method <green>{methodDisplayName}</green> was automatically selected for <gray>{provider}</gray>.</white>");
		VerificationMessages.Commands.Confirm.RecoveryCodes recoveryCodes =
				new VerificationMessages.Commands.Confirm.RecoveryCodes();
		recoveryCodes.setLayout(VerificationMessages.Commands.Confirm.RecoveryCodes.Layout.TWO_COLUMN);
		recoveryCodes.setBody(List.of(
				" ",
				" <green><bold>Identica</bold>",
				"  <white>Use these recovery codes if you lose access</white>",
				"  <white>to your authenticator application.</white>",
				" ",
				"  <white>Store them in a safe place before continuing:</white>",
				"{entries}",
				" ",
				"  <click:run_command:/2fa enroll confirm saved><green>[CONFIRM]</green></click>  " +
						"<click:run_command:/2fa enroll cancel><red>[CANCEL]</red></click>",
				" "
		));
		VerificationMessages.Commands.EntryFormat recoveryEntry = new VerificationMessages.Commands.EntryFormat();
		recoveryEntry.setFormat("   <gray>{code}</gray>");
		recoveryEntry.setEmptyFormat("   <gray>{code}</gray>");
		recoveryCodes.setSingleColumnEntry(recoveryEntry);
		VerificationMessages.Commands.EntryFormat twoColumnRecoveryEntry = new VerificationMessages.Commands.EntryFormat();
		twoColumnRecoveryEntry.setFormat("   <gray>{left}</gray>  <gray>{right}</gray>");
		twoColumnRecoveryEntry.setEmptyFormat("   <gray>{left}</gray>");
		recoveryCodes.setTwoColumnEntry(twoColumnRecoveryEntry);
		recoveryCodes.setEmpty("   <red>No recovery codes generated</red>");
		confirm.setRecoveryCodes(recoveryCodes);

		VerificationMessages.Commands.EntryFormat enrollmentEntry = new VerificationMessages.Commands.EntryFormat();
		enrollmentEntry.setFormat("  <dark_gray>▪</dark_gray> <aqua>{methodDisplayName}</aqua> <gray>[{enabledAt}]</gray>");
		enrollmentEntry.setEmptyFormat("  <dark_gray>▪</dark_gray> <aqua>{methodDisplayName}</aqua>");
		status.setEnrollmentEntry(enrollmentEntry);

		VerificationMessages.Commands.EntryFormat selectionEntry = new VerificationMessages.Commands.EntryFormat();
		selectionEntry.setFormat("  <dark_gray>▪</dark_gray> <white>{provider}</white>: <green>{methodDisplayName}</green>");
		selectionEntry.setEmptyFormat("  <dark_gray>▪</dark_gray> <white>{provider}</white>");
		status.setSelectionEntry(selectionEntry);

		VerificationMessages.Commands.Use use = new VerificationMessages.Commands.Use();
		use.setProviderNotFound("{prefix}<white>Unknown provider <gray>{provider}</gray>.</white>");
		use.setProviderUnsupported("{prefix}<white>Provider <gray>{provider}</gray> does not support verification.</white>");
		use.setProviderVerificationDisabled("{prefix}<white>Verification is disabled for <gray>{provider}</gray>.</white>");
		use.setMethodNotEnrolled("{prefix}<white>Method <gray>{methodDisplayName}</gray> is not enrolled.</white>");
		use.setMethodDisabledForProvider("{prefix}<white>Method <gray>{methodDisplayName}</gray> is not enabled for <gray>{provider}</gray>.</white>");
		use.setAlreadySelected("{prefix}<white><green>{methodDisplayName}</green> is already selected for <gray>{provider}</gray>.</white>");
		use.setUpdated("{prefix}<white><green>{methodDisplayName}</green> selected for <gray>{provider}</gray>.</white>");

		VerificationMessages.Commands.Disable disable = new VerificationMessages.Commands.Disable();
		disable.setMethodNotEnrolled("{prefix}<white>Method <gray>{methodDisplayName}</gray> is not enrolled.</white>");
		disable.setProtectedPrompt("{prefix}<white>Confirm the code from your authenticator with <yellow>/2fa confirm</yellow> <gray>[Code]</gray> to disable <gray>{methodDisplayName}</gray>.</white>");
		disable.setDisabled("{prefix}<white>Verification method <red>{methodDisplayName}</red> disabled.</white>");

		VerificationMessages.Commands.Cancel cancel = new VerificationMessages.Commands.Cancel();
		cancel.setNoPending("{prefix}<white>No pending verification action.</white>");
		cancel.setCancelled("{prefix}<white>Pending verification enrollment cancelled.</white>");
		cancel.setCancelledProtectedAction("{prefix}<white>Pending protected action cancelled.</white>");

		VerificationMessages.Methods.Totp totp = new VerificationMessages.Methods.Totp();
		totp.setPending(List.of(
				" ",
				" <green><bold>Identica</bold>",
				" ",
				"  <white>Scan or enter this TOTP secret:</white>",
				"   <gray>{secret}</gray>",
				" ",
				"  <click:open_url:'https://api.qrserver.com/v1/create-qr-code/?size=200x200&data={uriEncoded}'><green>[OPEN QR CODE]</green></click>",
				" ",
				"  <white>Use <yellow>/2fa enroll confirm</yellow> <gray>[Code]</gray> to continue.</white>",
				" "
		));
		VerificationMessages.Methods methods = new VerificationMessages.Methods();
		methods.setTotp(totp);

		messages.setMethods(methods);
		commands.setStatus(status);
		commands.setEnroll(enroll);
		commands.setConfirm(confirm);
		commands.setUse(use);
		commands.setDisable(disable);
		commands.setCancel(cancel);
		messages.setCommands(commands);
		return messages;
	}
}
