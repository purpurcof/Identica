package me.whereareiam.identica.service;

import me.whereareiam.identica.model.migration.*;
import me.whereareiam.identica.model.migration.operation.*;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;

/**
 * Service for orchestrating provider migration flows.
 */
public interface MigrationService {
	@NotNull MigrationResult request(@NotNull MigrationRequest request);

	@NotNull MigrationResult confirm(@NotNull MigrationConfirm confirm);

	@NotNull MigrationResult start(@NotNull MigrationStart start);

	@NotNull MigrationResult cancel(@NotNull MigrationCancel cancel);

	@NotNull Optional<PendingMigration> findPendingMigration(@NotNull UUID connectionUniqueId);
}
