package me.whereareiam.identica.provider.premium.step;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import me.whereareiam.identica.auth.step.type.SeamlessStep;
import me.whereareiam.identica.identity.registry.IdentityRegistry;
import me.whereareiam.identica.model.auth.AuthContext;
import me.whereareiam.identica.model.auth.StepResult;
import me.whereareiam.identica.model.identity.IdentityState;
import me.whereareiam.identica.provider.premium.config.PremiumMessages;
import me.whereareiam.identica.type.HandshakeMode;
import me.whereareiam.identica.util.UniqueIdGenerator;

import java.util.List;
import java.util.UUID;
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
		PremiumMessages.Verification verification = messagesProvider.get().getVerification();
		String username = context.getUsername();
		String ip = context.getIp();
		if (username == null || username.isBlank() || ip == null || ip.isBlank())
			return CompletableFuture.completedFuture(failed(verification));

		IdentityState state = context.getIdenticaUniqueId() != null
				? identityRegistry.findState(context.getIdenticaUniqueId()).orElse(null)
				: null;
		String providerSubject = state != null ? state.getProviderSubject() : null;
		if (providerSubject == null || providerSubject.isBlank())
			return CompletableFuture.completedFuture(failed(verification));

		UUID offlineUuid = UniqueIdGenerator.offlinePlayerUniqueId(username);
		if (offlineUuid != null && providerSubject.equalsIgnoreCase(offlineUuid.toString()))
			return CompletableFuture.completedFuture(requireReconnect(verification));

		return CompletableFuture.completedFuture(completeWithProfile(context, providerSubject, username));
	}

	private StepResult completeWithProfile(AuthContext context, String providerSubject, String username) {
		AuthContext.Provider provider = AuthContext.Provider.builder()
				.providerId("premium")
				.providerSubject(providerSubject)
				.providerUsername(username)
				.build();

		context.setProvider(provider);

		return StepResult.complete(context);
	}

	private StepResult requireReconnect(PremiumMessages.Verification verification) {
		String message = joinLines(preferRejoinMessage(verification));
		return StepResult.requireReconnect(HandshakeMode.ONLINE, message);
	}

	private StepResult failed(PremiumMessages.Verification verification) {
		return StepResult.failed(joinLines(verification.getInvalidSession()));
	}

	private List<String> preferRejoinMessage(PremiumMessages.Verification verification) {
		List<String> rejoin = verification.getRejoin();
		return rejoin != null && !rejoin.isEmpty()
				? rejoin
				: verification.getInvalidSession();
	}

	private String joinLines(List<String> lines) {
		return String.join("\n", lines);
	}
}
