package me.whereareiam.identica.provider.capability.offline.bootstrap;

import me.whereareiam.identica.model.provider.capability.ProviderCapabilityDescriptor;
import me.whereareiam.identica.provider.capability.bootstrap.ProviderCapabilityBootstrap;
import me.whereareiam.identica.provider.capability.offline.type.OfflineCapability;
import org.jetbrains.annotations.NotNull;

/**
 * Bootstrap for the built-in offline capability.
 */
public final class OfflineCapabilityBootstrap implements ProviderCapabilityBootstrap {
	public static final @NotNull OfflineCapabilityBootstrap INSTANCE = new OfflineCapabilityBootstrap();

	@Override
	public @NotNull ProviderCapabilityDescriptor descriptor() {
		return ProviderCapabilityDescriptor.builder()
				.capability(OfflineCapability.CAPABILITY)
				.build();
	}
}
