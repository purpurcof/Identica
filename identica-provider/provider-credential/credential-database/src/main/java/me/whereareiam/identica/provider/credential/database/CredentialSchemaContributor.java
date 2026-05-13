package me.whereareiam.identica.provider.credential.database;

import me.whereareiam.dialectica.SchemaManager;
import me.whereareiam.identica.database.schema.SchemaContributor;
import org.jetbrains.annotations.NotNull;

public final class CredentialSchemaContributor implements SchemaContributor {
	@Override
	public void contribute(@NotNull SchemaManager schemaManager) {
		schemaManager
				.scanPackages("me.whereareiam.identica.provider.credential.database.entity")
				.registerMigrationScope("credential-accounts", scope -> scope
						.scanPackages("me.whereareiam.identica.provider.credential.database.migration"));
	}
}
