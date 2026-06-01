package me.whereareiam.identica.provider.capability.migration.bootstrap;

import me.whereareiam.identica.model.provider.capability.ProviderCapabilityDescriptor;
import me.whereareiam.identica.provider.capability.bootstrap.ProviderCapabilityBootstrap;
import me.whereareiam.identica.provider.capability.migration.type.MigrationCapability;
import org.jetbrains.annotations.NotNull;

/**
 * Bootstrap for the built-in migration capability.
 */
public final class MigrationCapabilityBootstrap implements ProviderCapabilityBootstrap {
	public static final @NotNull MigrationCapabilityBootstrap INSTANCE = new MigrationCapabilityBootstrap();

	@Override
	public @NotNull ProviderCapabilityDescriptor descriptor() {
		return ProviderCapabilityDescriptor.builder()
				.capability(MigrationCapability.CAPABILITY)
				.build();
	}
}
