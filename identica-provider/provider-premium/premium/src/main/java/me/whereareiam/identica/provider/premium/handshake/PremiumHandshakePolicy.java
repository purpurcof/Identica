package me.whereareiam.identica.provider.premium.handshake;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.auth.HandshakePolicy;
import me.whereareiam.identica.model.auth.handshake.HandshakeDecision;
import me.whereareiam.identica.model.auth.handshake.HandshakeRequest;
import me.whereareiam.identica.provider.premium.config.PremiumSettings;
import me.whereareiam.identica.provider.premium.profile.PremiumProfileLookup;
import me.whereareiam.identica.provider.premium.type.VerificationFlow;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class PremiumHandshakePolicy implements HandshakePolicy {
	private final Provider<PremiumSettings> settingsProvider;
	private final PremiumProfileLookup profileLookup;

	@Override
	public CompletionStage<HandshakeDecision> evaluate(HandshakeRequest request) {
		String username = request != null
				? request.getIdentity().getUsername()
				: null;

		if (username == null || username.isBlank())
			return CompletableFuture.completedFuture(HandshakeDecision.allow());

		PremiumSettings.Verification verification = settingsProvider.get().getVerification();
		if (verification.getIntent() != VerificationFlow.SILENT)
			return CompletableFuture.completedFuture(HandshakeDecision.allow());

		return profileLookup.hasPremiumProfile(username)
				.thenApply(hasProfile -> hasProfile ? HandshakeDecision.forceOnline() : HandshakeDecision.allow());
	}
}
