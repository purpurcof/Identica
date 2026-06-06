package me.whereareiam.identica.provider.capability.authoritative.username;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.provider.capability.authoritative.username.config.provider.AuthoritativeUsernameMessagesProvider;
import me.whereareiam.identica.provider.capability.authoritative.username.conflict.UsernameConflictType;
import me.whereareiam.identica.provider.capability.authoritative.username.conflict.factory.UsernameConflictContextFactory;
import me.whereareiam.identica.provider.capability.authoritative.username.conflict.resolver.UsernameConflictResolver;
import me.whereareiam.identica.provider.capability.authoritative.username.model.AuthoritativeUsernameMessages;
import me.whereareiam.identica.provider.capability.authoritative.username.pipeline.AuthoritativeUsernamePipelineExtension;
import me.whereareiam.identica.provider.capability.authoritative.username.pipeline.prepare.*;
import me.whereareiam.identica.provider.capability.authoritative.username.pipeline.scenario.authentication.SynchronizeAuthenticationUsernamePhase;
import me.whereareiam.identica.provider.capability.authoritative.username.pipeline.scenario.migration.SynchronizeMigrationUsernamePhase;
import me.whereareiam.identica.provider.capability.authoritative.username.pipeline.scenario.registration.SynchronizeRegistrationUsernamePhase;

import java.nio.file.Path;

@RequiredArgsConstructor
public class AuthoritativeUsernameGlobalModule extends AbstractModule {
	private final Path capabilityPath;

	@Override
	protected void configure() {
		bind(AuthoritativeUsernameMessagesProvider.class).asEagerSingleton();
		bind(AuthoritativeUsernameMessages.class).toProvider(AuthoritativeUsernameMessagesProvider.class);

		bind(UsernameConflictContextFactory.class).asEagerSingleton();
		bind(UsernameConflictResolver.class).asEagerSingleton();
		bind(UsernameConflictType.class).asEagerSingleton();

		bind(ApplyAuthoritativeUsernamePhase.class).asEagerSingleton();
		bind(SynchronizeAuthenticationUsernamePhase.class).asEagerSingleton();
		bind(SynchronizeRegistrationUsernamePhase.class).asEagerSingleton();
		bind(SynchronizeMigrationUsernamePhase.class).asEagerSingleton();
		bind(ReviewAuthenticationUsernamePhase.class).asEagerSingleton();
		bind(ReviewRegistrationUsernamePhase.class).asEagerSingleton();
		bind(PersistAuthenticationUsernamePhase.class).asEagerSingleton();
		bind(PersistRegistrationUsernamePhase.class).asEagerSingleton();
		bind(AuthoritativeUsernamePipelineExtension.class).asEagerSingleton();
	}

	@Provides
	@Singleton
	@Named("authoritativeUsernameCapabilityPath")
	Path provideCapabilityPath() {
		return capabilityPath;
	}
}
