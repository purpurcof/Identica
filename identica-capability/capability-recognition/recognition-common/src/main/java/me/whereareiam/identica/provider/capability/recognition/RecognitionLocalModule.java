package me.whereareiam.identica.provider.capability.recognition;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import me.whereareiam.identica.provider.capability.contribution.ProviderCapabilityContribution;
import me.whereareiam.identica.provider.capability.recognition.compatibility.restriction.join.RecognitionJoinSignalContribution;

public class RecognitionLocalModule extends AbstractModule {
	@Override
	protected void configure() {
		Multibinder.newSetBinder(binder(), ProviderCapabilityContribution.class)
				.addBinding()
				.to(RecognitionJoinSignalContribution.class);
	}
}
