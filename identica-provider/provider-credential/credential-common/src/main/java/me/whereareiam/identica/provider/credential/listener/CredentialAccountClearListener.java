package me.whereareiam.identica.provider.credential.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.whereareiam.identica.database.provider.ProviderLinkPersistenceService;
import me.whereareiam.identica.event.EventListener;
import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.event.account.AccountLifecycleEvent;
import me.whereareiam.identica.event.base.IdenticEvent;
import me.whereareiam.identica.provider.credential.CredentialConstants;
import me.whereareiam.identica.provider.credential.account.CredentialAccountService;
import me.whereareiam.identica.type.event.EventOrder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@Singleton
public class CredentialAccountClearListener implements EventListener {
	private final CredentialAccountService credentialService;
	private final ProviderLinkPersistenceService providerLinkPersistenceService;

	@Inject
	public CredentialAccountClearListener(
			@NotNull CredentialAccountService credentialService,
			@NotNull ProviderLinkPersistenceService providerLinkPersistenceService,
			@NotNull EventManager eventManager
	) {
		this.credentialService = credentialService;
		this.providerLinkPersistenceService = providerLinkPersistenceService;
		eventManager.register(this);
	}

	@IdenticEvent(EventOrder.LOW)
	public void onAccountLifecycle(@NotNull AccountLifecycleEvent event) {
		deleteByUniqueId(event.getIdentity().getUniqueId());
	}

	private void deleteByUniqueId(@Nullable UUID uniqueId) {
		if (uniqueId == null) return;

		for (var link : providerLinkPersistenceService.findByUniqueId(uniqueId)) {
			if (link == null) continue;
			if (!CredentialConstants.PROVIDER_ID.equalsIgnoreCase(link.getProviderId()))
				continue;

			String subject = link.getProviderSubject();
			if (subject.isBlank()) continue;

			credentialService.delete(subject);
		}
	}
}
