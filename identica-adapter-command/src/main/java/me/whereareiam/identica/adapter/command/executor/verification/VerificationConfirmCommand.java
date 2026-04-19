package me.whereareiam.identica.adapter.command.executor.verification;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import me.whereareiam.identica.ConnectionCoordinator;
import me.whereareiam.identica.Serializer;
import me.whereareiam.identica.annotation.Argument;
import me.whereareiam.identica.annotation.Command;
import me.whereareiam.identica.annotation.Definition;
import me.whereareiam.identica.command.ProtectedActionCommand;
import me.whereareiam.identica.identity.actor.Identity;
import me.whereareiam.identica.identity.session.SessionService;
import me.whereareiam.identica.model.auth.ConnectionDecision;
import me.whereareiam.identica.model.auth.request.AdvanceRequest;
import me.whereareiam.identica.model.config.Messages;
import me.whereareiam.identica.model.config.Verification;
import me.whereareiam.identica.model.pipeline.journey.JourneyStateItem;
import me.whereareiam.identica.model.pipeline.state.PipelineState;
import me.whereareiam.identica.model.pipeline.state.PipelineStateReference;
import me.whereareiam.identica.model.pipeline.verification.VerificationDisablePendingState;
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
public class VerificationConfirmCommand extends ProtectedActionCommand<Void> {
	private final Provider<Messages> messagesProvider;
	private final Provider<Verification> verificationProvider;
	private final VerificationService verificationService;
	private final PipelineStateStore pipelineStateStore;
	private final ConnectionCoordinator connectionCoordinator;
	private final VerificationMessagePresenter messagePresenter;
	private final SessionService sessionService;

	@Inject
	public VerificationConfirmCommand(
			Provider<Messages> messagesProvider,
			Provider<Verification> verificationProvider,
			VerificationService verificationService,
			PipelineStateStore pipelineStateStore,
			ConnectionCoordinator connectionCoordinator,
			VerificationMessagePresenter messagePresenter,
			SessionService sessionService
	) {
		super(verificationService);
		this.messagesProvider = messagesProvider;
		this.verificationProvider = verificationProvider;
		this.verificationService = verificationService;
		this.pipelineStateStore = pipelineStateStore;
		this.connectionCoordinator = connectionCoordinator;
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

	@Definition("verification-confirm")
	@Command("2fa confirm <input>")
	public void confirm(@NotNull Actor sender, @Argument("input") String input) {
		Identity identity = requireIdentity(sender, verificationMessages().getPlayerOnly());
		if (identity == null) return;

		if (submitProtectedAction(identity, input))
			return;

		if (!submitChallenge(identity, input))
			sendMessage(sender, verificationMessages().getConfirm().getNoPending(), Map.of());
	}

	private boolean submitProtectedAction(@NotNull Identity identity, @NotNull String input) {
		VerificationDisablePendingState pendingDisable = findPendingDisable(identity);
		if (pendingDisable == null)
			return false;

		StepUpResult result = confirmStepUp(identity.getUniqueId(), input, "disable-method");
		switch (result.getStatus()) {
			case INVALID_CODE -> sendMessage(identity, verificationMessages().getConfirm().getInvalidCode(), Map.of());
			case CURRENT_SESSION_REQUIRED -> sendMessage(identity, verificationMessages().getConfirm().getProtectedActionSessionRequired(), Map.of());
			case SELECTION_REQUIRED -> sendMessage(identity, verificationMessages().getConfirm().getProtectedActionSelectionRequired(), Map.of());
			case VERIFIED -> {
				clearPendingDisable(identity);
				if (pendingDisable.getMethodId().isBlank()) {
					sendMessage(identity, verificationMessages().getConfirm().getNoPending(), Map.of());
				} else {
					messagePresenter.presentDisableResult(identity, verificationService.disableMethod(identity.getUniqueId(), pendingDisable.getMethodId()));
				}
			}
			default -> {
			}
		}

		return true;
	}

	private boolean submitChallenge(@NotNull Identity identity, @NotNull String input) {
		PipelineStateReference reference = reference(identity);
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

	private @NotNull PipelineStateReference reference(@NotNull Identity identity) {
		return PipelineStateReference.builder()
				.connectionUniqueId(identity.getUniqueId())
				.identityUniqueId(identity.getUniqueId())
				.build();
	}

	private @Nullable VerificationDisablePendingState findPendingDisable(@NotNull Identity identity) {
		return pipelineStateStore.find(reference(identity))
				.flatMap(state -> state.item(VerificationDisablePendingState.class))
				.orElse(null);
	}

	private void clearPendingDisable(@NotNull Identity identity) {
		PipelineStateReference reference = reference(identity);
		PipelineState state = pipelineStateStore.find(reference).orElse(null);
		if (state == null || state.item(VerificationDisablePendingState.class).isEmpty())
			return;

		state.removeItem(VerificationDisablePendingState.class);
		if (state.getItems().isEmpty()) {
			pipelineStateStore.clear(reference);
			return;
		}

		long ttlMs = verificationProvider.get().challengeTtlMillis();
		pipelineStateStore.save(reference, state, ttlMs);
	}
}
