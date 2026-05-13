package me.whereareiam.identica.provider.credential.database.migration;

import me.whereareiam.dialectica.migration.SchemaMigrationContext;

final class CredentialAccountMigrationTables {
	static final String CREDENTIALS_TABLE = "identica_provider_credential_accounts";
	static final String CREDENTIAL_HISTORY_TABLE = "identica_provider_credential_accounts_history";

	static void renameTable(
			SchemaMigrationContext context,
			String sourceTable,
			String targetTable
	) {
		boolean sourceExists = context.tableExists(sourceTable);
		boolean targetExists = context.tableExists(targetTable);

		if (!sourceExists)
			return;
		if (targetExists) {
			throw new IllegalStateException(
					"Cannot rename %s to %s because both tables already exist".formatted(sourceTable, targetTable)
			);
		}

		context.renameTable(sourceTable, targetTable);
	}
}
