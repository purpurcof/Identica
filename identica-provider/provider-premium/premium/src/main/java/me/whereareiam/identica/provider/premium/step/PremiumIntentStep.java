package me.whereareiam.identica.provider.premium.step;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.whereareiam.identica.type.HandshakeMode;
import me.whereareiam.identica.auth.step.type.InteractiveStep;
import me.whereareiam.identica.model.auth.AuthContext;
import me.whereareiam.identica.model.auth.StepResult;
import me.whereareiam.identica.provider.premium.config.PremiumMessages;
import me.whereareiam.identica.provider.premium.config.PremiumSettings;
import me.whereareiam.identica.provider.premium.profile.PremiumProfileLookup;
import me.whereareiam.identica.provider.premium.type.VerificationFlow;

import java.util.concurrent.CompletableFuture;
import com.google.inject.Provider;

@Singleton
public class PremiumIntentStep extends InteractiveStep {
	private final Provider<PremiumSettings> settingsProvider;
	private final Provider<PremiumMessages> messagesProvider;
	private final PremiumProfileLookup profileLookup;

	@Inject
	public PremiumIntentStep(
			Provider<PremiumSettings> settingsProvider,
			Provider<PremiumMessages> messagesProvider,
			PremiumProfileLookup profileLookup
	) {
		super("premium_intent");
		this.settingsProvider = settingsProvider;
		this.messagesProvider = messagesProvider;
		this.profileLookup = profileLookup;
	}

	@Override
	public CompletableFuture<StepResult> execute(AuthContext context) {
		if (context.isOnlineMode()) return CompletableFuture.completedFuture(StepResult.proceed(context));

		PremiumMessages.Verification verificationMessages = messagesProvider.get().getVerification();
		return profileLookup.hasPremiumProfile(context.getUsername())
				.thenApply(hasProfile -> {
					if (!hasProfile) return StepResult.failed(null);
					if (settingsProvider.get().getVerification().getIntent() == VerificationFlow.SILENT)
						return StepResult.requireReconnect(
								HandshakeMode.ONLINE,
								String.join("\n", verificationMessages.getInvalidSession())
						);

					return StepResult.waiting(verificationMessages.getPrompt());
				});
	}
}
