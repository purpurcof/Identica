package me.whereareiam.identica.provider.credential.migration;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.provider.credential.config.CredentialMessages;
import me.whereareiam.identica.provider.migration.MigrationPrecheckContext;
import me.whereareiam.identica.provider.migration.MigrationPrecheckResult;
import me.whereareiam.identica.provider.migration.ProviderMigrationPrecheck;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class CredentialMigrationPrecheck implements ProviderMigrationPrecheck {
	private final Provider<CredentialMessages> messagesProvider;

	@Override
	public @NotNull MigrationPrecheckResult precheck(@NotNull MigrationPrecheckContext context) {
		CredentialMessages.Commands.Credential credential = messagesProvider.get().getCommands().getCredential();
		List<String> kick = credential.getConfirmed();
		String message = String.join("\n", kick);

		return MigrationPrecheckResult.allow(message);
	}
}
