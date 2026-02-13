package me.whereareiam.identica.migration;

import org.jetbrains.annotations.NotNull;

/**
 * Service for orchestrating provider migration flows.
 */
public interface MigrationService {
	@NotNull MigrationResult request(@NotNull MigrationRequest request);

	@NotNull MigrationResult confirm(@NotNull MigrationConfirm confirm);

	@NotNull MigrationResult start(@NotNull MigrationStart start);

	@NotNull MigrationResult cancel(@NotNull MigrationCancel cancel);
}
