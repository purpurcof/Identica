package me.whereareiam.identica.provider.credential.database.migration;

import me.whereareiam.dialectica.migration.SchemaMigration;
import me.whereareiam.dialectica.migration.SchemaMigrationContext;
import org.jetbrains.annotations.NotNull;

public final class V1RenameCrackedTables implements SchemaMigration {
	private static final String CREDENTIALS_TABLE = "identica_provider_credential_accounts";
	private static final String CREDENTIAL_HISTORY_TABLE = "identica_provider_credential_accounts_history";

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
		renameTable(context, "identica_cracked_accounts", CREDENTIALS_TABLE);
		renameTable(context, "identica_cracked_account_passwords", CREDENTIAL_HISTORY_TABLE);
	}

	private void renameTable(
			@NotNull SchemaMigrationContext context,
			@NotNull String sourceTable,
			@NotNull String targetTable
	) {
		if (!context.tableExists(sourceTable)) return;
		if (context.tableExists(targetTable))
			throw new IllegalStateException("Cannot rename %s to %s because both tables already exist".formatted(sourceTable, targetTable));

		context.renameTable(sourceTable, targetTable);
	}

}
