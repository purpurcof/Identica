package me.whereareiam.identica.feature.verification.config.defaults;

import com.google.inject.Singleton;
import me.whereareiam.configura.merge.defaults.DefaultsProvider;
import me.whereareiam.identica.feature.verification.config.VerificationCommands;

@Singleton
public class VerificationCommandsDefaults implements DefaultsProvider<VerificationCommands> {
	@Override
	public VerificationCommands supply(VerificationCommands commands) {
		new VerificationCommandDefinitions().register(commands.getCommands()::put);
		return commands;
	}
}
