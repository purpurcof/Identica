package me.whereareiam.identica.provider.capability.restriction.join;

import me.whereareiam.identica.type.provider.capability.ProviderCapability;
import org.jetbrains.annotations.NotNull;

/**
 * Built-in typed join restriction capability identifier.
 */
public final class JoinRestrictionCapability {
	public static final @NotNull ProviderCapability CAPABILITY = ProviderCapability.of("restriction-join");
}
