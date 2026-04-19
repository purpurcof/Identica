package me.whereareiam.identica.provider.premium.step;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import me.whereareiam.identica.database.provider.ProviderLinkPersistenceService;
import me.whereareiam.identica.model.config.Verification;
import me.whereareiam.identica.model.identity.provider.AccountProviderLink;
import me.whereareiam.identica.model.pipeline.journey.stage.step.StepResult;
import me.whereareiam.identica.model.pipeline.state.PipelineState;
import me.whereareiam.identica.model.pipeline.state.PipelineStateReference;
import me.whereareiam.identica.model.provider.ProviderContext;
import me.whereareiam.identica.model.verification.VerificationTarget;
import me.whereareiam.identica.model.verification.VerificationAttemptResult;
import me.whereareiam.identica.model.verification.challenge.VerificationChallengeAttempt;
import me.whereareiam.identica.pipeline.ScenarioContext;
import me.whereareiam.identica.pipeline.journey.step.type.InteractiveStep;
import me.whereareiam.identica.pipeline.state.PipelineStateStore;
import me.whereareiam.identica.provider.premium.config.PremiumMessages;
import me.whereareiam.identica.verification.VerificationService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Singleton
public class PremiumVerificationStep extends InteractiveStep {
	private final Provider<PremiumMessages> messagesProvider;
	private final Provider<Verification> verificationProvider;
	private final PipelineStateStore pipelineStateStore;
	private final ProviderLinkPersistenceService providerLinkPersistenceService;
	private final VerificationService verificationService;

	@Inject
	public PremiumVerificationStep(
			Provider<PremiumMessages> messagesProvider,
			Provider<Verification> verificationProvider,
			PipelineStateStore pipelineStateStore,
			ProviderLinkPersistenceService providerLinkPersistenceService,
			VerificationService verificationService
	) {
		super("premium-authentication-verification");
		this.messagesProvider = messagesProvider;
		this.verificationProvider = verificationProvider;
		this.pipelineStateStore = pipelineStateStore;
		this.providerLinkPersistenceService = providerLinkPersistenceService;
		this.verificationService = verificationService;
	}

	@Override
	public int order() {
		return 40;
	}

	@Override
	public @NotNull CompletableFuture<StepResult> execute(@NotNull ScenarioContext context) {
		ProviderContext provider = context.getProvider();
		if (provider == null || provider.getProviderId() == null || provider.getProviderSubject() == null)
			return CompletableFuture.completedFuture(StepResult.complete(context));

		UUID uniqueId = providerLinkPersistenceService.findBySubject(provider.getProviderId(), provider.getProviderSubject())
				.map(AccountProviderLink::getUniqueId)
				.orElse(null);

		if (uniqueId == null) return CompletableFuture.completedFuture(StepResult.complete(context));

		String input = consumeAttempt(context, verificationProvider.get().challengeTtlMillis());
		VerificationAttemptResult result = verificationService.verify(
				VerificationTarget.providerSelection(uniqueId, provider.getProviderId(), "authentication"),
				input
		);
		if (result.getStatus() == null)
			return CompletableFuture.completedFuture(StepResult.complete(context));

		PremiumMessages.Verification.Authentication messages = messagesProvider.get().getVerification().getAuthentication();
		return CompletableFuture.completedFuture(switch (result.getStatus()) {
			case PROVIDER_UNSUPPORTED, PROVIDER_VERIFICATION_DISABLED -> StepResult.complete(context);
			case METHOD_NOT_SELECTED -> result.isRequired()
					? StepResult.denied(messages.getRequired())
					: StepResult.complete(context);
			case INPUT_REQUIRED -> StepResult.waiting(joinLines(messages.getPrompt()));
			case VERIFIED -> StepResult.complete(context);
			case INVALID_INPUT -> StepResult.waiting(joinInvalid(messages));
			case METHOD_UNAVAILABLE -> StepResult.denied(messages.getUnavailable());
		});
	}

	private @Nullable String consumeAttempt(@NotNull ScenarioContext context, long ttlMs) {
		PipelineStateReference reference = PipelineStateReference.from(context);
		PipelineState stored = pipelineStateStore.find(reference).orElse(null);
		if (stored == null) return null;

		VerificationChallengeAttempt input = stored.item(VerificationChallengeAttempt.class).orElse(null);
		if (input == null) return null;

		stored.removeItem(VerificationChallengeAttempt.class);
		if (ttlMs > 0)
			pipelineStateStore.save(reference, stored, ttlMs);

		return input.getValue();
	}

	private String joinInvalid(PremiumMessages.Verification.Authentication messages) {
		String invalid = messages.getInvalid();
		String prompt = joinLines(messages.getPrompt());
		if (invalid.isBlank()) return prompt;
		if (prompt.isBlank()) return invalid;

		return invalid + "\n" + prompt;
	}

	private static @NotNull String joinLines(@Nullable List<String> lines) {
		if (lines == null || lines.isEmpty()) return "";
		return String.join("\n", lines);
	}
}
