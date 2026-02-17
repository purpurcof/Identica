package me.whereareiam.identica.provider.cracked.migration;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.provider.cracked.config.CrackedMessages;
import me.whereareiam.identica.provider.migration.MigrationPrecheckContext;
import me.whereareiam.identica.provider.migration.MigrationPrecheckResult;
import me.whereareiam.identica.provider.migration.ProviderMigrationPrecheck;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class CrackedMigrationPrecheck implements ProviderMigrationPrecheck {
	private final Provider<CrackedMessages> messagesProvider;

	@Override
	public @NotNull MigrationPrecheckResult precheck(@NotNull MigrationPrecheckContext context) {
		CrackedMessages.Commands.Cracked cracked = messagesProvider.get().getCommands().getCracked();
		List<String> kick = cracked.getConfirmed();
		String message = String.join("\n", kick);
		return MigrationPrecheckResult.allow(message);
	}
}
