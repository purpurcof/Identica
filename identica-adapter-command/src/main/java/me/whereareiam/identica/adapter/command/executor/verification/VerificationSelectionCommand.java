package me.whereareiam.identica.adapter.command.executor.verification;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import me.whereareiam.identica.annotation.Argument;
import me.whereareiam.identica.annotation.Command;
import me.whereareiam.identica.annotation.Definition;
import me.whereareiam.identica.annotation.Suggestions;
import me.whereareiam.identica.adapter.command.suggestion.ProviderIdSuggestions;
import me.whereareiam.identica.adapter.command.suggestion.VerificationMethodSuggestions;
import me.whereareiam.identica.command.ProtectedActionCommand;
import me.whereareiam.identica.identity.session.SessionService;
import me.whereareiam.identica.model.Session;
import me.whereareiam.identica.model.config.Messages;
import me.whereareiam.identica.model.config.Verification;
import me.whereareiam.identica.model.pipeline.state.PipelineState;
import me.whereareiam.identica.model.pipeline.state.PipelineStateReference;
import me.whereareiam.identica.model.pipeline.verification.VerificationDisablePendingState;
import me.whereareiam.identica.model.verification.VerificationResolutionRequest;
import me.whereareiam.identica.pipeline.state.PipelineStateStore;
import me.whereareiam.identica.identity.actor.Identity;
import me.whereareiam.identica.verification.VerificationService;
import me.whereareiam.keystone.Actor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Singleton
public class VerificationSelectionCommand extends ProtectedActionCommand<Void> {
	private final Provider<Messages> messagesProvider;
	private final Provider<Verification> verificationProvider;
	private final VerificationService verificationService;
	private final VerificationResultRenderer resultRenderer;
	private final PipelineStateStore pipelineStateStore;
	private final SessionService sessionService;

	@Inject
	public VerificationSelectionCommand(
			Provider<Messages> messagesProvider,
			Provider<Verification> verificationProvider,
			VerificationService verificationService,
			VerificationResultRenderer resultRenderer,
			PipelineStateStore pipelineStateStore,
			SessionService sessionService
	) {
		super(verificationService);
		this.messagesProvider = messagesProvider;
		this.verificationProvider = verificationProvider;
		this.verificationService = verificationService;
		this.resultRenderer = resultRenderer;
		this.pipelineStateStore = pipelineStateStore;
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

	@Definition("verification-use")
	@Command("2fa use <provider> <method>")
	public void use(
			@NotNull Actor sender,
			@Argument("provider") @Suggestions(ProviderIdSuggestions.KEY) String providerId,
			@Argument("method") @Suggestions(VerificationMethodSuggestions.KEY) String methodId
	) {
		Identity identity = requireIdentity(sender, messagesProvider.get().getCommands().getVerification().getPlayerOnly());
		if (identity == null) return;
		if (requireCurrentSession(identity) == null) return;

		resultRenderer.presentSelectionResult(sender, verificationService.selectMethod(identity.getUniqueId(), providerId, methodId));
	}

	@Definition("verification-disable")
	@Command("2fa disable <method>")
	public void disable(
			@NotNull Actor sender,
			@Argument("method") @Suggestions(VerificationMethodSuggestions.KEY) String methodId
	) {
		Identity identity = requireIdentity(sender, messagesProvider.get().getCommands().getVerification().getPlayerOnly());
		if (identity == null) return;
		if (requireCurrentSession(identity) == null) return;

		boolean enrolled = verificationService.findEnrollments(identity.getUniqueId()).stream()
				.anyMatch(entry -> entry != null && methodId.equalsIgnoreCase(entry.getMethodId()));
		if (!enrolled) {
			resultRenderer.presentDisableResult(sender, verificationService.disableMethod(identity.getUniqueId(), methodId));
			return;
		}

		long ttlMs = verificationProvider.get().challengeTtlMillis();
		PipelineStateReference reference = reference(identity);
		PipelineState state = pipelineStateStore.find(reference).orElse(PipelineState.initial());
		state.putItem(new VerificationDisablePendingState(methodId, System.currentTimeMillis()), ttlMs);
		pipelineStateStore.save(reference, state, ttlMs);
		verificationService.resolveVerification(VerificationResolutionRequest.builder()
				.uniqueId(identity.getUniqueId())
				.providerId(sessionService.findByUniqueId(identity.getUniqueId()).join()
						.map(Session::getProviderId)
						.orElse(""))
				.purpose("disable-method")
				.build());

		resultRenderer.presentDisablePrompt(sender, methodId);
	}

	private @NotNull PipelineStateReference reference(@NotNull Identity identity) {
		return PipelineStateReference.builder()
				.connectionUniqueId(identity.getUniqueId())
				.identityUniqueId(identity.getUniqueId())
				.build();
	}
}
