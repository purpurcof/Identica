package me.whereareiam.identica.provider.premium.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.event.EventListener;
import me.whereareiam.identica.event.base.IdenticEvent;
import me.whereareiam.identica.event.auth.AuthDecisionEvent;
import me.whereareiam.identica.model.auth.AuthContext;
import me.whereareiam.identica.model.auth.AuthDecision;
import me.whereareiam.identica.model.auth.IdentityClaim;
import me.whereareiam.identica.provider.premium.database.PremiumIdentityPersistenceService;

import java.util.UUID;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class PremiumIdentityListener implements EventListener {
	private final PremiumIdentityPersistenceService identityService;

	@IdenticEvent
	public void onAuthDecision(AuthDecisionEvent event) {
		AuthDecision decision = event.getDecision();
		if (decision == null || decision.getStatus() != AuthDecision.Status.ALLOW)
			return;

		AuthContext context = event.getContext();
		if (context == null) return;

		IdentityClaim claim = context.getIdentityClaim();
		if (claim == null || !"Premium".equalsIgnoreCase(claim.getProviderId()))
			return;

		UUID identicaUniqueId = context.getIdenticaUniqueId();
		if (identicaUniqueId == null) return;

		String providerSubject = claim.getProviderSubject();
		if (providerSubject == null || providerSubject.isBlank())
			return;

		try {
			identityService.upsert(identicaUniqueId, UUID.fromString(providerSubject));
		} catch (IllegalArgumentException ignored) {
		}
	}
}
