package me.whereareiam.identica.provider.cracked;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import me.whereareiam.identica.model.config.Commands;
import me.whereareiam.identica.provider.cracked.account.AutoupgradeLifecycle;
import me.whereareiam.identica.provider.migration.ProviderMigrationPrecheck;
import me.whereareiam.identica.provider.cracked.account.CrackedAccountService;
import me.whereareiam.identica.provider.cracked.account.DefaultCrackedAccountService;
import me.whereareiam.identica.provider.cracked.command.ManagementCommand;
import me.whereareiam.identica.provider.cracked.command.ChangePasswordCommand;
import me.whereareiam.identica.provider.cracked.command.LoginCommand;
import me.whereareiam.identica.provider.cracked.command.CrackedCommand;
import me.whereareiam.identica.provider.cracked.command.PassCommand;
import me.whereareiam.identica.provider.cracked.config.CrackedCommands;
import me.whereareiam.identica.provider.cracked.config.CrackedMessages;
import me.whereareiam.identica.provider.cracked.config.CrackedSettings;
import me.whereareiam.identica.provider.cracked.config.provider.CrackedCommandsProvider;
import me.whereareiam.identica.provider.cracked.config.provider.CrackedMessagesProvider;
import me.whereareiam.identica.provider.cracked.config.provider.CrackedSettingsProvider;
import me.whereareiam.identica.provider.cracked.database.DatabaseModule;
import me.whereareiam.identica.provider.cracked.cryptography.CryptographyModule;
import me.whereareiam.identica.provider.cracked.cryptography.CryptographyService;
import me.whereareiam.identica.provider.cracked.cryptography.DefaultCryptographyService;
import me.whereareiam.identica.provider.cracked.cryptography.argon2id.Argon2IdCryptographyModule;
import me.whereareiam.identica.provider.cracked.cryptography.bcrypt.BcryptCryptographyModule;
import me.whereareiam.identica.provider.cracked.ratelimit.DefaultRateLimitService;
import me.whereareiam.identica.provider.cracked.ratelimit.RateLimitService;
import me.whereareiam.identica.provider.cracked.migration.CrackedMigrationPrecheck;
import me.whereareiam.identica.provider.cracked.util.PasswordRules;

public class CrackedConfiguration extends AbstractModule {
	@Override
	protected void configure() {
		install(new DatabaseModule());

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

		install(new CryptographyModule());
		install(new BcryptCryptographyModule());
		install(new Argon2IdCryptographyModule());
		bind(CrackedAccountService.class).to(DefaultCrackedAccountService.class).asEagerSingleton();
		bind(CryptographyService.class).to(DefaultCryptographyService.class).asEagerSingleton();
		bind(AutoupgradeLifecycle.class).asEagerSingleton();
		bind(PasswordRules.class).asEagerSingleton();
		bind(RateLimitService.class).to(DefaultRateLimitService.class).asEagerSingleton();

		Multibinder.newSetBinder(binder(), Object.class, Names.named("crackedCommandInstances"))
				.addBinding()
				.to(PassCommand.class);
		Multibinder.newSetBinder(binder(), Object.class, Names.named("crackedCommandInstances"))
				.addBinding()
				.to(LoginCommand.class);
		Multibinder.newSetBinder(binder(), Object.class, Names.named("crackedCommandInstances"))
				.addBinding()
				.to(ChangePasswordCommand.class);
		Multibinder.newSetBinder(binder(), Object.class, Names.named("crackedCommandInstances"))
				.addBinding()
				.to(CrackedCommand.class);
		Multibinder.newSetBinder(binder(), Object.class, Names.named("crackedCommandInstances"))
				.addBinding()
				.to(ManagementCommand.class);

		Multibinder.newSetBinder(binder(), ProviderMigrationPrecheck.class)
				.addBinding()
				.to(CrackedMigrationPrecheck.class);
	}
}
