package me.whereareiam.identica.provider.premium;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.auth.HandshakePolicy;
import me.whereareiam.identica.model.auth.handshake.HandshakeDecision;
import me.whereareiam.identica.model.auth.handshake.HandshakeRequest;
import me.whereareiam.identica.model.config.Settings;
import me.whereareiam.identica.provider.premium.profile.PremiumProfileLookup;
import me.whereareiam.identica.type.step.AuthFlowType;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class PremiumHandshakePolicy implements HandshakePolicy {
	private final Provider<Settings> settingsProvider;
	private final PremiumProfileLookup profileLookup;

	@Override
	public CompletionStage<HandshakeDecision> evaluate(HandshakeRequest request) {
		String username = request != null
				? request.getIdentity().getUsername()
				: null;

		if (username == null || username.isBlank())
			return CompletableFuture.completedFuture(HandshakeDecision.allow());

		AuthFlowType flow = settingsProvider.get().getAuthentication().getFlow();
		if (flow != AuthFlowType.SEAMLESS) return CompletableFuture.completedFuture(HandshakeDecision.allow());

		return profileLookup.hasPremiumProfile(username)
				.thenApply(hasProfile -> hasProfile
						? HandshakeDecision.forceOnline()
						: HandshakeDecision.allow());
	}
}
