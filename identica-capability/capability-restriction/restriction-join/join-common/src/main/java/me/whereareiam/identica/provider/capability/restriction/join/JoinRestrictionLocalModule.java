package me.whereareiam.identica.provider.capability.restriction.join;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import me.whereareiam.identica.provider.capability.contribution.ProviderCapabilityContribution;

public class JoinRestrictionLocalModule extends AbstractModule {
	@Override
	protected void configure() {
		Multibinder.newSetBinder(binder(), ProviderCapabilityContribution.class)
				.addBinding()
				.to(LinkedJoinSignalContribution.class);
	}
}
