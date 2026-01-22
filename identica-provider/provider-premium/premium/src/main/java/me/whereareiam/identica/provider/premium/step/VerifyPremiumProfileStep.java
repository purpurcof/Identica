package me.whereareiam.identica.provider.premium.step;

import me.whereareiam.identica.auth.step.type.SeamlessStep;
import me.whereareiam.identica.model.auth.AuthContext;
import me.whereareiam.identica.model.auth.IdentityClaim;
import me.whereareiam.identica.model.auth.StepResult;
import me.whereareiam.identica.provider.premium.config.PremiumMessages;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import com.google.inject.Provider;

public class VerifyPremiumProfileStep extends SeamlessStep {
	private final Provider<PremiumMessages> messagesProvider;

	public VerifyPremiumProfileStep(Provider<PremiumMessages> messagesProvider) {
		super("verify");
		this.messagesProvider = messagesProvider;
	}

	@Override
	public CompletableFuture<StepResult> execute(AuthContext context) {
		if (!context.isOnlineMode()) {
			PremiumMessages.Verification verification = messagesProvider.get().getVerification();
			String invalidSession = String.join("\n", verification.getInvalidSession());
			return CompletableFuture.completedFuture(StepResult.failed(invalidSession));
		}
		String profileId = context.getProfileUniqueId();
		String username = context.getUsername();
		if (profileId == null || profileId.isBlank() || username == null || username.isBlank()) {
			PremiumMessages.Verification verification = messagesProvider.get().getVerification();
			String invalidSession = String.join("\n", verification.getInvalidSession());
			return CompletableFuture.completedFuture(StepResult.failed(invalidSession));
		}

		return CompletableFuture.completedFuture(completeWithProfile(context, profileId, username));
	}

	private StepResult completeWithProfile(AuthContext context, String profileId, String username) {
		IdentityClaim claim = IdentityClaim.builder()
				.providerId("Premium")
				.providerSubject(profileId)
				.conflictData(Map.of(
						"username", username,
						"mojangUniqueId", profileId
				))
				.build();

		context.setIdentityClaim(claim);

		return StepResult.complete(context);
	}

}
