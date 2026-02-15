package me.whereareiam.identica.provider.cracked;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import me.whereareiam.identica.model.config.Commands;
import me.whereareiam.identica.provider.cracked.account.AutoupgradeLifecycle;
import me.whereareiam.identica.provider.cracked.account.CrackedAccountService;
import me.whereareiam.identica.provider.cracked.account.DefaultCrackedAccountService;
import me.whereareiam.identica.provider.cracked.command.ChangePasswordCommand;
import me.whereareiam.identica.provider.cracked.command.CrackedCommand;
import me.whereareiam.identica.provider.cracked.command.LoginCommand;
import me.whereareiam.identica.provider.cracked.command.ManagementCommand;
import me.whereareiam.identica.provider.cracked.command.PassCommand;
import me.whereareiam.identica.provider.cracked.config.CrackedCommands;
import me.whereareiam.identica.provider.cracked.config.CrackedMessages;
import me.whereareiam.identica.provider.cracked.config.CrackedSettings;
import me.whereareiam.identica.provider.cracked.config.provider.CrackedCommandsProvider;
import me.whereareiam.identica.provider.cracked.config.provider.CrackedMessagesProvider;
import me.whereareiam.identica.provider.cracked.config.provider.CrackedSettingsProvider;
import me.whereareiam.identica.provider.cracked.listener.AccountClearCrackedListener;
import me.whereareiam.identica.provider.cracked.migration.CrackedMigrationPrecheck;
import me.whereareiam.identica.provider.cracked.ratelimit.DefaultRateLimitService;
import me.whereareiam.identica.provider.cracked.ratelimit.RateLimitService;
import me.whereareiam.identica.provider.cracked.util.PasswordRules;
import me.whereareiam.identica.provider.migration.ProviderMigrationPrecheck;

public class CommonConfiguration extends AbstractModule {
	@Override
	protected void configure() {
		bind(CrackedSettingsProvider.class).asEagerSingleton();
		bind(CrackedSettings.class).toProvider(CrackedSettingsProvider.class);

		bind(CrackedMessagesProvider.class).asEagerSingleton();
		bind(CrackedMessages.class).toProvider(CrackedMessagesProvider.class);

		bind(CrackedCommandsProvider.class).asEagerSingleton();
		bind(CrackedCommands.class)
				.annotatedWith(Names.named("cracked"))
				.toProvider(CrackedCommandsProvider.class);
		bind(Commands.class)
				.annotatedWith(Names.named("cracked"))
				.toProvider(CrackedCommandsProvider.class);

		bind(CrackedAccountService.class).to(DefaultCrackedAccountService.class).asEagerSingleton();
		bind(AccountClearCrackedListener.class).asEagerSingleton();
		bind(AutoupgradeLifecycle.class).asEagerSingleton();
		bind(PasswordRules.class).asEagerSingleton();
		bind(RateLimitService.class).to(DefaultRateLimitService.class).asEagerSingleton();

		Multibinder<Object> crackedCommandInstances = Multibinder.newSetBinder(
				binder(),
				Object.class,
				Names.named("crackedCommandInstances")
		);
		crackedCommandInstances.addBinding().to(PassCommand.class);
		crackedCommandInstances.addBinding().to(LoginCommand.class);
		crackedCommandInstances.addBinding().to(ChangePasswordCommand.class);
		crackedCommandInstances.addBinding().to(CrackedCommand.class);
		crackedCommandInstances.addBinding().to(ManagementCommand.class);

		Multibinder.newSetBinder(binder(), ProviderMigrationPrecheck.class)
				.addBinding()
				.to(CrackedMigrationPrecheck.class);
	}
}
