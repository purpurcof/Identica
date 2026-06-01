package me.whereareiam.identica.provider.capability.verification.bootstrap;

import me.whereareiam.identica.model.provider.capability.ProviderCapabilityDescriptor;
import me.whereareiam.identica.provider.capability.bootstrap.ProviderCapabilityBootstrap;
import me.whereareiam.identica.provider.capability.verification.type.VerificationCapability;
import org.jetbrains.annotations.NotNull;

/**
 * Bootstrap for the built-in verification capability.
 */
public final class VerificationCapabilityBootstrap implements ProviderCapabilityBootstrap {
	public static final @NotNull VerificationCapabilityBootstrap INSTANCE = new VerificationCapabilityBootstrap();

	@Override
	public @NotNull ProviderCapabilityDescriptor descriptor() {
		return ProviderCapabilityDescriptor.builder()
				.capability(VerificationCapability.CAPABILITY)
				.build();
	}
}
