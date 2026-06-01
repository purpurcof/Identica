package me.whereareiam.identica.provider.capability.authoritative.username.bootstrap;

import me.whereareiam.identica.model.provider.capability.ProviderCapabilityDescriptor;
import me.whereareiam.identica.provider.capability.authoritative.username.type.AuthoritativeUsernameCapability;
import me.whereareiam.identica.provider.capability.bootstrap.ProviderCapabilityBootstrap;
import org.jetbrains.annotations.NotNull;

/**
 * Bootstrap for the built-in authoritative username capability.
 */
public final class AuthoritativeUsernameCapabilityBootstrap implements ProviderCapabilityBootstrap {
	public static final @NotNull AuthoritativeUsernameCapabilityBootstrap INSTANCE =
			new AuthoritativeUsernameCapabilityBootstrap();

	@Override
	public @NotNull ProviderCapabilityDescriptor descriptor() {
		return ProviderCapabilityDescriptor.builder()
				.capability(AuthoritativeUsernameCapability.CAPABILITY)
				.build();
	}
}
