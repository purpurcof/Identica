package me.whereareiam.identica.provider.premium;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.attributes.AttributeScope;
import me.whereareiam.identica.attributes.ScopedAttributes;
import me.whereareiam.identica.auth.HandshakePolicy;
import me.whereareiam.identica.database.ProviderLinkPersistenceService;
import me.whereareiam.identica.model.auth.handshake.HandshakeDecision;
import me.whereareiam.identica.model.auth.handshake.HandshakeRequest;
import me.whereareiam.identica.provider.premium.profile.PremiumProfileLookup;
import me.whereareiam.identica.util.UniqueIdGenerator;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class PremiumHandshakePolicy implements HandshakePolicy {
	private final PremiumProfileLookup profileLookup;
	private final ProviderLinkPersistenceService providerLinkPersistenceService;
	private final ScopedAttributes scopedAttributes;

	@Override
	public CompletionStage<HandshakeDecision> evaluate(HandshakeRequest request) {
		String username = request != null
				? request.getIdentity().getUsername()
				: null;

		if (username == null || username.isBlank())
			return CompletableFuture.completedFuture(HandshakeDecision.allow());

		if (hasPremiumLinkByProfileId(username))
			return CompletableFuture.completedFuture(HandshakeDecision.forceOnline());

		return profileLookup.hasPremiumProfile(username)
				.thenApply(hasProfile -> hasProfile
						? HandshakeDecision.forceOnline()
						: HandshakeDecision.allow());
	}

	private boolean hasPremiumLinkByProfileId(String username) {
		String profileId = scopedAttributes.get(AttributeScope.PROFILE_HINT, username, PremiumKeys.PLATFORM_PROFILE_ID).orElse(null);
		if (profileId == null || profileId.isBlank())
			return false;

		UUID offlineUuid = UniqueIdGenerator.offlinePlayerUniqueId(username);
		if (offlineUuid != null && profileId.equalsIgnoreCase(offlineUuid.toString()))
			return false;

		return providerLinkPersistenceService.findBySubject(PremiumConstants.PROVIDER_ID, profileId).isPresent();
	}
}
