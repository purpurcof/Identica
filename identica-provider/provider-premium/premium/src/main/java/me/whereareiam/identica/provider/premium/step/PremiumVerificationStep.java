package me.whereareiam.identica.provider.premium.step;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import me.whereareiam.identica.database.provider.ProviderLinkPersistenceService;
import me.whereareiam.identica.model.identity.provider.AccountProviderLink;
import me.whereareiam.identica.model.pipeline.journey.stage.step.StepResult;
import me.whereareiam.identica.model.provider.ProviderContext;
import me.whereareiam.identica.model.verification.VerificationGateRequest;
import me.whereareiam.identica.model.verification.VerificationGateResult;
import me.whereareiam.identica.pipeline.ScenarioContext;
import me.whereareiam.identica.pipeline.journey.step.type.InteractiveStep;
import me.whereareiam.identica.provider.premium.config.PremiumMessages;
import me.whereareiam.identica.type.verification.VerificationGateStatus;
import me.whereareiam.identica.verification.VerificationService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Singleton
public class PremiumVerificationStep extends InteractiveStep {
	private final Provider<PremiumMessages> messagesProvider;
	private final ProviderLinkPersistenceService providerLinkPersistenceService;
	private final VerificationService verificationService;

	@Inject
	public PremiumVerificationStep(
			Provider<PremiumMessages> messagesProvider,
			ProviderLinkPersistenceService providerLinkPersistenceService,
			VerificationService verificationService
	) {
		super("premium-authentication-verification");
		this.messagesProvider = messagesProvider;
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

		VerificationGateResult result = verificationService.evaluateGate(VerificationGateRequest.builder()
				.uniqueId(uniqueId)
				.providerId(provider.getProviderId())
				.purpose("authentication")
				.build());

		PremiumMessages.Verification.Authentication messages = messagesProvider.get().getVerification().getAuthentication();
		return CompletableFuture.completedFuture(toStepResult(result, context, messages));
	}

	private StepResult toStepResult(
			@NotNull VerificationGateResult result,
			@NotNull ScenarioContext context,
			PremiumMessages.Verification.@NotNull Authentication messages
	) {
		VerificationGateStatus status = result.getStatus();
		if (status == VerificationGateStatus.SATISFIED || status == VerificationGateStatus.SKIPPED)
			return StepResult.complete(context);
		if (status == VerificationGateStatus.WAITING)
			return StepResult.waiting(joinLines(messages.getPrompt()));

		if (status == VerificationGateStatus.DENIED)
			return StepResult.denied(result.getMethodId() == null
					? messages.getRequired()
					: messages.getUnavailable());

		return StepResult.failed("");
	}

	private static @NotNull String joinLines(@Nullable List<String> lines) {
		if (lines == null || lines.isEmpty()) return "";
		return String.join("\n", lines);
	}
}
