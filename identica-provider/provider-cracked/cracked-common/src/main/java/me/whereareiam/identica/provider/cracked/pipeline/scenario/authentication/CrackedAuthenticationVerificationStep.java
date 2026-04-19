package me.whereareiam.identica.provider.cracked.pipeline.scenario.authentication;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import me.whereareiam.identica.database.provider.ProviderLinkPersistenceService;
import me.whereareiam.identica.model.config.Verification;
import me.whereareiam.identica.model.identity.provider.AccountProviderLink;
import me.whereareiam.identica.model.pipeline.journey.stage.step.StepResult;
import me.whereareiam.identica.model.pipeline.state.PipelineState;
import me.whereareiam.identica.model.pipeline.state.PipelineStateReference;
import me.whereareiam.identica.model.verification.challenge.VerificationChallengeAttempt;
import me.whereareiam.identica.model.verification.challenge.VerificationChallengeResult;
import me.whereareiam.identica.pipeline.ScenarioContext;
import me.whereareiam.identica.pipeline.state.PipelineStateStore;
import me.whereareiam.identica.provider.cracked.CrackedConstants;
import me.whereareiam.identica.provider.cracked.config.CrackedMessages;
import me.whereareiam.identica.provider.cracked.pipeline.scenario.AbstractCrackedStep;
import me.whereareiam.identica.verification.VerificationService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Singleton
public class CrackedAuthenticationVerificationStep extends AbstractCrackedStep {
	private final Provider<CrackedMessages> messagesProvider;
	private final Provider<Verification> verificationProvider;
	private final PipelineStateStore pipelineStateStore;
	private final ProviderLinkPersistenceService providerLinkPersistenceService;
	private final VerificationService verificationService;

	@Inject
	public CrackedAuthenticationVerificationStep(
			Provider<CrackedMessages> messagesProvider,
			Provider<Verification> verificationProvider,
			PipelineStateStore pipelineStateStore,
			ProviderLinkPersistenceService providerLinkPersistenceService,
			VerificationService verificationService
	) {
		super("cracked-authentication-verification");
		this.messagesProvider = messagesProvider;
		this.verificationProvider = verificationProvider;
		this.pipelineStateStore = pipelineStateStore;
		this.providerLinkPersistenceService = providerLinkPersistenceService;
		this.verificationService = verificationService;
	}

	@Override
	public int order() {
		return 20;
	}

	@Override
	public @NotNull CompletableFuture<StepResult> execute(@NotNull ScenarioContext context) {
		String providerSubject = requireProviderSubject(context);
		UUID uniqueId = providerLinkPersistenceService.findBySubject(CrackedConstants.PROVIDER_ID, providerSubject)
				.map(AccountProviderLink::getUniqueId)
				.orElse(null);

		if (uniqueId == null) return CompletableFuture.completedFuture(StepResult.complete(context));

		String input = consumeAttempt(context, verificationProvider.get().challengeTtlMillis());
		VerificationChallengeResult result = verificationService.challenge(uniqueId, CrackedConstants.PROVIDER_ID, input);
		if (result.getStatus() == null) return CompletableFuture.completedFuture(StepResult.complete(context));

		CrackedMessages.Scenario.Authentication.Verification messages = messagesProvider.get()
				.getScenario()
				.getAuthentication()
				.getVerification();

		return CompletableFuture.completedFuture(switch (result.getStatus()) {
			case SKIP, ALLOW -> StepResult.complete(context);
			case WAITING -> StepResult.waiting(joinLines(messages.getPrompt()));
			case INVALID_INPUT -> StepResult.waiting(joinInvalid(messages));
			case REQUIRED_MISSING -> StepResult.denied(messages.getRequired());
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
		if (ttlMs > 0) pipelineStateStore.save(reference, stored, ttlMs);

		return input.getValue();
	}

	private String joinInvalid(CrackedMessages.Scenario.Authentication.Verification messages) {
		String invalid = messages.getInvalid();
		String prompt = joinLines(messages.getPrompt());
		if (invalid == null || invalid.isBlank()) return prompt;
		if (prompt.isBlank()) return invalid;

		return invalid + "\n" + prompt;
	}

	private static @NotNull String joinLines(@Nullable List<String> lines) {
		if (lines == null || lines.isEmpty()) return "";
		return String.join("\n", lines);
	}
}
