package me.whereareiam.identica.provider.premium;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import me.whereareiam.identica.model.config.Commands;
import me.whereareiam.identica.handshake.HandshakePolicy;
import me.whereareiam.identica.provider.eligibility.ProviderEligibilityResolver;
import me.whereareiam.identica.provider.premium.policy.PremiumHandshakePolicy;
import me.whereareiam.identica.provider.premium.resolver.PremiumEligibilityResolver;
import me.whereareiam.identica.provider.profile.ProfileSubjectResolver;
import me.whereareiam.identica.provider.premium.config.PremiumCommands;
import me.whereareiam.identica.provider.premium.command.PremiumCommand;
import me.whereareiam.identica.provider.premium.config.PremiumMessages;
import me.whereareiam.identica.provider.premium.config.PremiumSettings;
import me.whereareiam.identica.provider.premium.config.provider.PremiumCommandsProvider;
import me.whereareiam.identica.provider.premium.config.provider.PremiumMessagesProvider;
import me.whereareiam.identica.provider.premium.config.provider.PremiumSettingsProvider;
import me.whereareiam.identica.provider.premium.resolver.PremiumProfileSubjectResolver;

public class PremiumModule extends AbstractModule {
	@Override
	protected void configure() {
		// Configs
		bind(PremiumSettingsProvider.class).asEagerSingleton();
		bind(PremiumSettings.class).toProvider(PremiumSettingsProvider.class);
		bind(PremiumMessagesProvider.class).asEagerSingleton();
		bind(PremiumMessages.class).toProvider(PremiumMessagesProvider.class);
		bind(PremiumCommandsProvider.class).asEagerSingleton();

		bind(PremiumCommands.class)
				.annotatedWith(Names.named("premium"))
				.toProvider(PremiumCommandsProvider.class);

		bind(Commands.class)
				.annotatedWith(Names.named("premium"))
				.toProvider(PremiumCommandsProvider.class);

		Multibinder.newSetBinder(binder(), Object.class, Names.named("premiumCommandInstances"))
				.addBinding()
				.to(PremiumCommand.class);

		Multibinder.newSetBinder(binder(), HandshakePolicy.class)
				.addBinding()
				.to(PremiumHandshakePolicy.class);
		Multibinder.newSetBinder(binder(), ProviderEligibilityResolver.class)
				.addBinding()
				.to(PremiumEligibilityResolver.class);
		Multibinder.newSetBinder(binder(), ProfileSubjectResolver.class)
				.addBinding()
				.to(PremiumProfileSubjectResolver.class);
	}
}
