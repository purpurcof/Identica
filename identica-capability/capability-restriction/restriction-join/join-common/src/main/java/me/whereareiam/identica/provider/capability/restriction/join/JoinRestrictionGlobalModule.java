package me.whereareiam.identica.provider.capability.restriction.join;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.provider.capability.restriction.join.command.JoinRestrictionCommand;
import me.whereareiam.identica.provider.capability.restriction.join.config.JoinRestrictionCommands;
import me.whereareiam.identica.provider.capability.restriction.join.config.JoinRestrictionMessages;
import me.whereareiam.identica.provider.capability.restriction.join.config.provider.JoinRestrictionCommandsProvider;
import me.whereareiam.identica.provider.capability.restriction.join.config.provider.JoinRestrictionMessagesProvider;
import me.whereareiam.identica.provider.capability.restriction.join.config.provider.JoinRestrictionProvidersProvider;
import me.whereareiam.identica.provider.capability.restriction.join.pipeline.JoinPipelineExtension;
import me.whereareiam.identica.provider.capability.restriction.join.pipeline.prepare.ApplyJoinRestrictionPhase;
import me.whereareiam.identica.provider.capability.restriction.join.pipeline.scenario.EnforceAuthenticationJoinRestrictionPhase;
import me.whereareiam.identica.provider.capability.restriction.join.pipeline.scenario.EnforceRegistrationJoinRestrictionPhase;

import java.nio.file.Path;
import java.util.Set;

@RequiredArgsConstructor
public class JoinRestrictionGlobalModule extends AbstractModule {
	private final Path joinRestrictionCapabilityPath;

	@Override
	protected void configure() {
		bind(JoinRestrictionProvidersProvider.class).asEagerSingleton();
		bind(JoinRestrictionCommandsProvider.class).asEagerSingleton();
		bind(JoinRestrictionMessagesProvider.class).asEagerSingleton();
		bind(JoinRestrictionMessages.class).toProvider(JoinRestrictionMessagesProvider.class);
		bind(JoinRestrictionTypeResolver.class).asEagerSingleton();
		bind(ApplyJoinRestrictionPhase.class).asEagerSingleton();
		bind(EnforceAuthenticationJoinRestrictionPhase.class).asEagerSingleton();
		bind(EnforceRegistrationJoinRestrictionPhase.class).asEagerSingleton();
		bind(JoinPipelineExtension.class).asEagerSingleton();
		bind(JoinRestrictionCommand.class).asEagerSingleton();
		bind(CommandRegistrar.class).asEagerSingleton();
	}

	@Provides
	@Singleton
	@Named("joinRestrictionCapabilityPath")
	Path provideJoinRestrictionCapabilityPath() {
		return joinRestrictionCapabilityPath;
	}

	@Provides
	@Singleton
	@Named("joinRestriction")
	JoinRestrictionCommands provideJoinRestrictionCommands(JoinRestrictionCommandsProvider provider) {
		return provider.get();
	}

	@Provides
	@Singleton
	@Named("joinRestrictionMessages")
	JoinRestrictionMessages provideJoinRestrictionMessages(JoinRestrictionMessagesProvider provider) {
		return provider.get();
	}

	@Provides
	@Singleton
	@Named("joinRestrictionCommandInstances")
	Set<Object> provideJoinRestrictionCommandInstances(JoinRestrictionCommand command) {
		return Set.of(command);
	}
}
