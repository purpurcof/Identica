package me.whereareiam.identica.adapter.synchronization;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import me.whereareiam.identica.synchronization.SynchronizationService;

public class SynchronizationConfiguration extends AbstractModule {
	@Override
	protected void configure() {
		Multibinder.newSetBinder(binder(), SynchronizationService.class, Names.named("synchronizationProviders"))
				.addBinding()
				.to(RedisSynchronizationService.class);
	}
}
