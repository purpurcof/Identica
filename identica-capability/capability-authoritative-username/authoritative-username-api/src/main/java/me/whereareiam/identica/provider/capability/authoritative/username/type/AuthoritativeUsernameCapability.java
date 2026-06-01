package me.whereareiam.identica.provider.capability.authoritative.username.type;

import me.whereareiam.identica.type.provider.capability.ProviderCapability;
import org.jetbrains.annotations.NotNull;

/**
 * Built-in authoritative username capability identifier.
 */
public final class AuthoritativeUsernameCapability {
	public static final @NotNull ProviderCapability CAPABILITY = ProviderCapability.of("authoritative_username");
}
