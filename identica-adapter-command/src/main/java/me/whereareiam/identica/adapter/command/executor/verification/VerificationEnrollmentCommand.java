package me.whereareiam.identica.adapter.command.executor.verification;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.ConnectionCoordinator;
import me.whereareiam.identica.Serializer;
import me.whereareiam.identica.annotation.Argument;
import me.whereareiam.identica.annotation.Command;
import me.whereareiam.identica.annotation.Definition;
import me.whereareiam.identica.annotation.Suggestions;
import me.whereareiam.identica.adapter.command.suggestion.VerificationMethodSuggestions;
import me.whereareiam.identica.identity.actor.Identity;
import me.whereareiam.identica.identity.session.SessionService;
import me.whereareiam.identica.model.Session;
import me.whereareiam.identica.model.auth.ConnectionDecision;
import me.whereareiam.identica.model.auth.request.AdvanceRequest;
import me.whereareiam.identica.model.config.Messages;
import me.whereareiam.identica.model.config.Verification;
import me.whereareiam.identica.model.pipeline.journey.JourneyStateItem;
import me.whereareiam.identica.model.pipeline.state.PipelineState;
import me.whereareiam.identica.model.pipeline.state.PipelineStateReference;
import me.whereareiam.identica.model.verification.enrollment.PendingVerificationEnrollment;
import me.whereareiam.identica.model.verification.challenge.VerificationChallengeAttempt;
import me.whereareiam.identica.pipeline.state.PipelineStateStore;
import me.whereareiam.identica.verification.VerificationService;
import me.whereareiam.keystone.Actor;
import me.whereareiam.keystone.model.SerializerContent;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class VerificationEnrollmentCommand {
	private final Provider<Messages> messagesProvider;
	private final Provider<Verification> verificationProvider;
	private final VerificationService verificationService;
	private final SessionService sessionService;
	private final PipelineStateStore pipelineStateStore;
	private final ConnectionCoordinator connectionCoordinator;
	private final VerificationMessagePresenter messagePresenter;

	@Definition("verification-enroll")
	@Command("2fa enroll <method>")
	public void enroll(
			@NotNull Actor sender,
			@Argument("method") @Suggestions(VerificationMethodSuggestions.KEY) String methodId
	) {
		Identity identity = requireIdentity(sender);
		if (identity == null) return;

		String providerId = sessionService.findByUniqueId(identity.getUniqueId())
				.join()
				.map(Session::getProviderId)
				.orElse(null);

		messagePresenter.presentActionResult(sender, verificationService.beginEnrollment(
				identity.getUniqueId(),
				identity.getUsername(),
				providerId,
				methodId
		));
	}

	@Definition("verification-confirm")
	@Command("2fa confirm <input>")
	public void confirm(@NotNull Actor sender, @Argument("input") String input) {
		Identity identity = requireIdentity(sender);
		if (identity == null) return;

		PendingVerificationEnrollment pendingEnrollment = verificationService.findPendingEnrollment(identity.getUniqueId()).orElse(null);
		if (pendingEnrollment != null) {
			messagePresenter.presentActionResult(sender, verificationService.confirmEnrollment(identity.getUniqueId(), input));
			return;
		}

		if (!submitChallenge(identity, input))
			sendMessage(sender, verificationMessages().getConfirm().getNoPending(), Map.of());
	}

	@Definition("verification-cancel")
	@Command("2fa cancel")
	public void cancel(@NotNull Actor sender) {
		Identity identity = requireIdentity(sender);
		if (identity == null) return;

		if (verificationService.cancelPendingEnrollment(identity.getUniqueId())) {
			sendMessage(sender, verificationMessages().getCancel().getCancelled(), Map.of());
			return;
		}

		sendMessage(sender, verificationMessages().getCancel().getNoPending(), Map.of());
	}

	private boolean submitChallenge(@NotNull Identity identity, @NotNull String input) {
		PipelineStateReference reference = PipelineStateReference.builder()
				.connectionUniqueId(identity.getUniqueId())
				.identityUniqueId(identity.getUniqueId())
				.build();
		PipelineState state = pipelineStateStore.find(reference).orElse(null);
		if (state == null || state.item(JourneyStateItem.class).isEmpty()) return false;

		long ttlMs = verificationProvider.get().challengeTtlMillis();
		state.putItem(VerificationChallengeAttempt.builder().value(input).build(), ttlMs);
		pipelineStateStore.save(reference, state, ttlMs);

		ConnectionDecision decision = connectionCoordinator.advance(AdvanceRequest.builder()
				.connectionUniqueId(identity.getUniqueId())
				.identity(identity)
				.build()).toCompletableFuture().join();
		if (decision == null || decision.getStatus() == null) return true;

		switch (decision.getStatus()) {
			case WAIT -> sendMessage(identity, decision.getMessage(), Map.of());
			case DENY, REQUIRE_RECONNECT -> disconnect(identity, decision.getMessage());
			case NO_PENDING -> sendMessage(identity, verificationMessages().getConfirm().getNoPending(), Map.of());
			default -> {
			}
		}

		return true;
	}

	private @Nullable Identity requireIdentity(@NotNull Actor sender) {
		if (sender instanceof Identity identity)
			return identity;
		sendMessage(sender, verificationMessages().getPlayerOnly(), Map.of());
		return null;
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

	private void disconnect(@NotNull Actor sender, @Nullable String message) {
		if (message == null || message.isBlank()) return;
		Component component = Serializer.serialize(sender, message);
		if (sender instanceof Identity identity)
			identity.disconnect(component);
	}
}
