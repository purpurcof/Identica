package me.whereareiam.identica.provider.capability.online.bootstrap;

import me.whereareiam.identica.model.provider.capability.ProviderCapabilityDescriptor;
import me.whereareiam.identica.provider.capability.bootstrap.ProviderCapabilityBootstrap;
import me.whereareiam.identica.provider.capability.online.type.OnlineCapability;
import org.jetbrains.annotations.NotNull;

/**
 * Bootstrap for the built-in online capability.
 */
public final class OnlineCapabilityBootstrap implements ProviderCapabilityBootstrap {
	public static final @NotNull OnlineCapabilityBootstrap INSTANCE = new OnlineCapabilityBootstrap();

	@Override
	public @NotNull ProviderCapabilityDescriptor descriptor() {
		return ProviderCapabilityDescriptor.builder()
				.capability(OnlineCapability.CAPABILITY)
				.build();
	}
}
