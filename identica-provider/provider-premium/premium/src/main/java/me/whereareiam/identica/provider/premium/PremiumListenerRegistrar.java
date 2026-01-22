package me.whereareiam.identica.provider.premium;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.provider.premium.listener.PremiumAccountLinkResolver;
import me.whereareiam.identica.provider.premium.listener.PremiumIdentityListener;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class PremiumListenerRegistrar {
	private final EventManager eventManager;
	private final PremiumIdentityListener identityListener;
	private final PremiumAccountLinkResolver accountLinkResolver;

	public void register() {
		eventManager.register(identityListener);
		eventManager.register(accountLinkResolver);
	}

	public void unregister() {
		eventManager.unregister(identityListener);
		eventManager.unregister(accountLinkResolver);
	}
}
