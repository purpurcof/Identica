package me.whereareiam.identica.provider.premium.migration;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.provider.migration.MigrationPrecheckContext;
import me.whereareiam.identica.provider.migration.MigrationPrecheckResult;
import me.whereareiam.identica.provider.migration.ProviderMigrationPrecheck;
import me.whereareiam.identica.provider.premium.config.PremiumMessages;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class PremiumMigrationPrecheck implements ProviderMigrationPrecheck {
	private final Provider<PremiumMessages> messagesProvider;

	@Override
	public @NotNull MigrationPrecheckResult precheck(@NotNull MigrationPrecheckContext context) {
		PremiumMessages.Commands.Premium premium = messagesProvider.get().getCommands().getPremium();
		List<String> kick = premium.getConfirmed();
		String message = String.join("\n", kick);
		return MigrationPrecheckResult.allow(message);
	}
}
