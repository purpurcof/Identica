package me.whereareiam.identica.provider.premium.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.event.EventListener;
import me.whereareiam.identica.event.account.AccountLinkResolveEvent;
import me.whereareiam.identica.event.base.IdenticEvent;
import me.whereareiam.identica.model.auth.IdentityClaim;
import me.whereareiam.identica.provider.premium.database.PremiumIdentityPersistenceService;

import java.util.UUID;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class PremiumAccountLinkResolver implements EventListener {
	private final PremiumIdentityPersistenceService identityService;

	@IdenticEvent
	public void onResolve(AccountLinkResolveEvent event) {
		if (event == null || event.getUniqueId() != null) return;

		IdentityClaim claim = event.getClaim();
		if (claim == null || !"Premium".equalsIgnoreCase(claim.getProviderId()))
			return;

		String providerSubject = claim.getProviderSubject();
		if (providerSubject == null || providerSubject.isBlank()) return;

		UUID mojangUniqueId;
		try {
			mojangUniqueId = UUID.fromString(providerSubject);
		} catch (IllegalArgumentException ignored) {
			return;
		}

		identityService.findIdenticaUniqueId(mojangUniqueId)
				.ifPresent(event::setUniqueId);
	}
}
