package me.whereareiam.identica.provider.migration;

import org.jetbrains.annotations.NotNull;

/**
 * Provider hook invoked before starting migration.
 */
public interface ProviderMigrationPrecheck {
	@NotNull MigrationPrecheckResult precheck(@NotNull MigrationPrecheckContext context);
}
