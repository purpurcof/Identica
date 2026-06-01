package me.whereareiam.identica.provider.capability.migration.type;

import me.whereareiam.identica.type.provider.capability.ProviderCapability;
import org.jetbrains.annotations.NotNull;

/**
 * Built-in migration capability identifier.
 */
public final class MigrationCapability {
	public static final @NotNull ProviderCapability CAPABILITY = ProviderCapability.of("migration");
}
