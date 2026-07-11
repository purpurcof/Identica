package me.whereareiam.identica.common.messaging;

import com.google.inject.AbstractModule;
import me.whereareiam.identica.service.DeliveryService;
import me.whereareiam.identica.service.DeliveryStore;

public class MessagingConfiguration extends AbstractModule {
	@Override
	protected void configure() {
		bind(DeliveryStore.class).to(DefaultDeliveryStore.class).asEagerSingleton();
		bind(DeliveryService.class).to(DefaultDeliveryService.class).asEagerSingleton();
		bind(DeliveryLifecycle.class).asEagerSingleton();
	}
}
