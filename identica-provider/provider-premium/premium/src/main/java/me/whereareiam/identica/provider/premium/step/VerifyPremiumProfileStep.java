package me.whereareiam.identica.provider.premium.step;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import me.whereareiam.identica.auth.step.type.SeamlessStep;
import me.whereareiam.identica.model.auth.AuthContext;
import me.whereareiam.identica.model.auth.StepResult;
import me.whereareiam.identica.provider.premium.config.PremiumMessages;
import me.whereareiam.identica.model.identity.IdentityState;
import me.whereareiam.identica.registry.IdentityRegistry;

import java.util.concurrent.CompletableFuture;

@Singleton
public class VerifyPremiumProfileStep extends SeamlessStep {
	private final Provider<PremiumMessages> messagesProvider;
	private final IdentityRegistry identityRegistry;

	@Inject
	public VerifyPremiumProfileStep(
			Provider<PremiumMessages> messagesProvider,
			IdentityRegistry identityRegistry
	) {
		super("verify");
		this.messagesProvider = messagesProvider;
		this.identityRegistry = identityRegistry;
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

		IdentityState state = context.getIdenticaUniqueId() != null
				? identityRegistry.findState(context.getIdenticaUniqueId()).orElse(null)
				: null;
		String profileUniqueId = state != null ? state.getProfileUniqueId() : null;
		if (profileUniqueId == null || profileUniqueId.isBlank()) {
			PremiumMessages.Verification verification = messagesProvider.get().getVerification();
			String invalidSession = String.join("\n", verification.getInvalidSession());
			return CompletableFuture.completedFuture(StepResult.failed(invalidSession));
		}

		return CompletableFuture.completedFuture(completeWithProfile(context, profileUniqueId, username));
	}

	private StepResult completeWithProfile(AuthContext context, String profileUniqueId, String username) {
		AuthContext.Provider provider = AuthContext.Provider.builder()
				.providerId("premium")
				.providerSubject(profileUniqueId)
				.providerUsername(username)
				.build();

		context.setProvider(provider);

		return StepResult.complete(context);
	}

}
