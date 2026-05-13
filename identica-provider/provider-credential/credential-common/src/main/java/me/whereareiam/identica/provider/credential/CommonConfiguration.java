package me.whereareiam.identica.provider.credential;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import me.whereareiam.identica.model.config.Commands;
import me.whereareiam.identica.provider.credential.account.AutoupgradeLifecycle;
import me.whereareiam.identica.provider.credential.account.CredentialAccountService;
import me.whereareiam.identica.provider.credential.account.DefaultCredentialAccountService;
import me.whereareiam.identica.provider.credential.command.*;
import me.whereareiam.identica.provider.credential.config.CredentialCommands;
import me.whereareiam.identica.provider.credential.config.CredentialMessages;
import me.whereareiam.identica.provider.credential.config.CredentialSettings;
import me.whereareiam.identica.provider.credential.config.provider.CredentialCommandsProvider;
import me.whereareiam.identica.provider.credential.config.provider.CredentialMessagesProvider;
import me.whereareiam.identica.provider.credential.config.provider.CredentialSettingsProvider;
import me.whereareiam.identica.provider.credential.listener.CredentialAccountClearListener;
import me.whereareiam.identica.provider.credential.migration.CredentialMigrationPrecheck;
import me.whereareiam.identica.provider.credential.sentinel.BruteForceSentinelDefinition;
import me.whereareiam.identica.provider.credential.sentinel.BruteForceSentinelLifecycle;
import me.whereareiam.identica.provider.credential.util.PasswordRules;
import me.whereareiam.identica.provider.migration.ProviderMigrationPrecheck;

public class CommonConfiguration extends AbstractModule {
	@Override
	protected void configure() {
		bind(CredentialSettingsProvider.class).asEagerSingleton();
		bind(CredentialSettings.class).toProvider(CredentialSettingsProvider.class);

		bind(CredentialMessagesProvider.class).asEagerSingleton();
		bind(CredentialMessages.class).toProvider(CredentialMessagesProvider.class);

		bind(CredentialCommandsProvider.class).asEagerSingleton();
		bind(CredentialCommands.class)
				.annotatedWith(Names.named("credential"))
				.toProvider(CredentialCommandsProvider.class);
		bind(Commands.class)
				.annotatedWith(Names.named("credential"))
				.toProvider(CredentialCommandsProvider.class);

		bind(CredentialAccountService.class).to(DefaultCredentialAccountService.class).asEagerSingleton();
		bind(CredentialAccountClearListener.class).asEagerSingleton();
		bind(AutoupgradeLifecycle.class).asEagerSingleton();
		bind(PasswordRules.class).asEagerSingleton();

		bind(BruteForceSentinelDefinition.class).asEagerSingleton();
		bind(BruteForceSentinelLifecycle.class).asEagerSingleton();

		Multibinder<Object> passwordCommandInstances = Multibinder.newSetBinder(
				binder(),
				Object.class,
				Names.named("credentialCommandInstances")
		);
		passwordCommandInstances.addBinding().to(PassCommand.class);
		passwordCommandInstances.addBinding().to(LoginCommand.class);
		passwordCommandInstances.addBinding().to(PasswordChangeCommand.class);
		passwordCommandInstances.addBinding().to(PasswordCommand.class);
		passwordCommandInstances.addBinding().to(ManagementCommand.class);

		Multibinder.newSetBinder(binder(), ProviderMigrationPrecheck.class)
				.addBinding()
				.to(CredentialMigrationPrecheck.class);
	}
}
