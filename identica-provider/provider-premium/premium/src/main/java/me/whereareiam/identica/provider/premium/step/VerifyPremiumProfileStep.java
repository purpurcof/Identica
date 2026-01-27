package me.whereareiam.identica.provider.premium.step;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.whereareiam.identica.auth.step.type.SeamlessStep;
import me.whereareiam.identica.model.auth.AuthContext;
import me.whereareiam.identica.model.auth.StepResult;
import me.whereareiam.identica.provider.premium.config.PremiumMessages;
import me.whereareiam.identica.session.PendingUniqueIdStore;
import me.whereareiam.identica.util.ProfileSubjectKey;

import java.util.concurrent.CompletableFuture;
import java.util.Optional;
import java.util.UUID;

@Singleton
public class VerifyPremiumProfileStep extends SeamlessStep {
	private final com.google.inject.Provider<PremiumMessages> messagesProvider;
	private final PendingUniqueIdStore pendingUniqueIdStore;

	@Inject
	public VerifyPremiumProfileStep(
			com.google.inject.Provider<PremiumMessages> messagesProvider,
			PendingUniqueIdStore pendingUniqueIdStore
	) {
		super("verify");
		this.messagesProvider = messagesProvider;
		this.pendingUniqueIdStore = pendingUniqueIdStore;
	}

	@Override
	public CompletableFuture<StepResult> execute(AuthContext context) {
		if (!context.isOnlineMode()) {
			PremiumMessages.Verification verification = messagesProvider.get().getVerification();
			String invalidSession = String.join("\n", verification.getInvalidSession());
			return CompletableFuture.completedFuture(StepResult.failed(invalidSession));
		}
		String username = context.getUsername();
		String ip = context.getIp();
		if (username == null || username.isBlank() || ip == null || ip.isBlank()) {
			PremiumMessages.Verification verification = messagesProvider.get().getVerification();
			String invalidSession = String.join("\n", verification.getInvalidSession());
			return CompletableFuture.completedFuture(StepResult.failed(invalidSession));
		}

		String key = ProfileSubjectKey.of(username, ip);
		if (key == null) {
			PremiumMessages.Verification verification = messagesProvider.get().getVerification();
			String invalidSession = String.join("\n", verification.getInvalidSession());
			return CompletableFuture.completedFuture(StepResult.failed(invalidSession));
		}

		return pendingUniqueIdStore.get(key)
				.thenApply(optional -> completeWithProfile(context, optional, username));
	}

	private StepResult completeWithProfile(AuthContext context, Optional<UUID> profileId, String username) {
		if (profileId.isEmpty()) {
			PremiumMessages.Verification verification = messagesProvider.get().getVerification();
			String invalidSession = String.join("\n", verification.getInvalidSession());
			return StepResult.failed(invalidSession);
		}

		AuthContext.Provider provider = AuthContext.Provider.builder()
				.providerId("premium")
				.providerSubject(profileId.get().toString())
				.providerUsername(username)
				.build();

		context.setProvider(provider);

		return StepResult.complete(context);
	}

}
