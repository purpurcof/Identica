package me.whereareiam.identica.adapter.synchronization;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.multibindings.OptionalBinder;
import com.google.inject.name.Names;
import me.whereareiam.identica.service.SynchronizationService;

public class SynchronizationConfiguration extends AbstractModule {
	@Override
	protected void configure() {
		OptionalBinder.newOptionalBinder(binder(), Key.get(SynchronizationService.class, Names.named("synchronizationProvider")))
				.setBinding()
				.to(RedisSynchronizationService.class);
	}
}
