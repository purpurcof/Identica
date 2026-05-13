package me.whereareiam.identica.provider.credential.database.migration;

import me.whereareiam.dialectica.migration.SchemaMigration;
import me.whereareiam.dialectica.migration.SchemaMigrationContext;
import org.jetbrains.annotations.NotNull;

public final class V1RenameCrackedTables implements SchemaMigration {
	@Override
	public int version() {
		return 1;
	}

	@Override
	public @NotNull String name() {
		return "rename_cracked_tables";
	}

	@Override
	public void migrate(@NotNull SchemaMigrationContext context) {
		CredentialAccountMigrationTables.renameTable(context, "identica_cracked_accounts", CredentialAccountMigrationTables.CREDENTIALS_TABLE);
		CredentialAccountMigrationTables.renameTable(context, "identica_cracked_accounts_passwords", CredentialAccountMigrationTables.CREDENTIAL_HISTORY_TABLE);
	}
}
