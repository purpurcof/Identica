package me.whereareiam.identica.adapter.command.executor.verification;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import me.whereareiam.identica.Serializer;
import me.whereareiam.identica.annotation.Argument;
import me.whereareiam.identica.annotation.Command;
import me.whereareiam.identica.annotation.Definition;
import me.whereareiam.identica.annotation.Suggestions;
import me.whereareiam.identica.adapter.command.suggestion.VerificationMethodSuggestions;
import me.whereareiam.identica.command.ProtectedActionCommand;
import me.whereareiam.identica.identity.actor.Identity;
import me.whereareiam.identica.identity.session.SessionService;
import me.whereareiam.identica.model.Session;
import me.whereareiam.identica.model.config.Messages;
import me.whereareiam.identica.model.verification.enrollment.VerificationEnrollmentSession;
import me.whereareiam.identica.verification.VerificationService;
import me.whereareiam.keystone.Actor;
import me.whereareiam.keystone.model.SerializerContent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

@Singleton
public class VerificationEnrollmentCommand extends ProtectedActionCommand<Void> {
	private final Provider<Messages> messagesProvider;
	private final VerificationService verificationService;
	private final VerificationMessagePresenter messagePresenter;
	private final SessionService sessionService;

	@Inject
	public VerificationEnrollmentCommand(
			Provider<Messages> messagesProvider,
			VerificationService verificationService,
			VerificationMessagePresenter messagePresenter,
			SessionService sessionService
	) {
		super(verificationService);
		this.messagesProvider = messagesProvider;
		this.verificationService = verificationService;
		this.messagePresenter = messagePresenter;
		this.sessionService = sessionService;
	}

	@Override
	protected @NotNull SessionService sessionService() {
		return sessionService;
	}

	@Override
	protected @Nullable String currentSessionRequiredMessage() {
		return messagesProvider.get().getCommands().getCurrentSessionRequired();
	}

	@Definition("verification-enroll")
	@Command("2fa enroll <method>")
	public void enroll(
			@NotNull Actor sender,
			@Argument("method") @Suggestions(VerificationMethodSuggestions.KEY) String methodId
	) {
		Identity identity = requireIdentity(sender, verificationMessages().getPlayerOnly());
		if (identity == null) return;
		Session session = requireCurrentSession(identity);
		if (session == null) return;

		messagePresenter.presentEnrollmentResult(sender, verificationService.beginEnrollment(
				identity.getUniqueId(),
				identity.getUsername(),
				session.getProviderId(),
				methodId
		));
	}

	@Definition("verification-enroll-confirm")
	@Command("2fa enroll confirm <input>")
	public void enrollConfirm(@NotNull Actor sender, @Argument("input") String input) {
		Identity identity = requireIdentity(sender, verificationMessages().getPlayerOnly());
		if (identity == null) return;

		VerificationEnrollmentSession pendingEnrollment = verificationService.findPendingEnrollment(identity.getUniqueId()).orElse(null);
		if (pendingEnrollment == null) {
			sendMessage(sender, verificationMessages().getConfirm().getNoPending(), Map.of());
			return;
		}

		messagePresenter.presentEnrollmentResult(sender, verificationService.confirmEnrollment(identity.getUniqueId(), input));
	}

	@Definition("verification-enroll-cancel")
	@Command("2fa enroll cancel")
	public void cancel(@NotNull Actor sender) {
		Identity identity = requireIdentity(sender, verificationMessages().getPlayerOnly());
		if (identity == null) return;

		if (verificationService.cancelPendingEnrollment(identity.getUniqueId())) {
			sendMessage(sender, verificationMessages().getCancel().getCancelled(), Map.of());
			return;
		}

		sendMessage(sender, verificationMessages().getCancel().getNoPending(), Map.of());
	}

	private Messages.Commands.Verification verificationMessages() {
		return messagesProvider.get().getCommands().getVerification();
	}

	private void sendMessage(@NotNull Actor sender, @Nullable String message, @NotNull Map<String, String> placeholders) {
		if (message == null || message.isBlank()) return;
		sender.sendMessage(Serializer.serialize(SerializerContent.builder()
				.receiver(sender)
				.message(message)
				.placeholders(placeholders)
				.build()));
	}
}
